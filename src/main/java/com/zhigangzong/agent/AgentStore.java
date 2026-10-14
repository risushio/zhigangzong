package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhigangzong.agent.AgentRecords.*;
import com.zhigangzong.common.PageQuery;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import com.zhigangzong.mapper.PortalMapper.Actor;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.function.Supplier;

@Service @RequiredArgsConstructor
public class AgentStore {
    private static final Logger log=LoggerFactory.getLogger(AgentStore.class);
    private final AgentMapper mapper;
    private final StudentProfileCreationService creation;
    private final ManagementMapper audit;
    private final AgentProperties properties;
    private final PlatformTransactionManager transactions;

    Actor actor() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        var actor=authentication==null?null:mapper.actor(authentication.getName());
        if(actor==null||!"SCHOOL_ADMIN".equals(actor.role())) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","无学生建档权限");
        return actor;
    }
    void enabled() { if(!properties.isEnabled()) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"AGENT_DISABLED","学生档案助手尚未启用"); }
    <T> T transaction(Supplier<T> work) {
        var template=new TransactionTemplate(transactions);template.setTimeout(properties.getToolTimeoutSeconds());
        // 学校/会话/草稿锁负责写入串行化；授权、字典及结果读取须看到刚提交的数据。
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try { return template.execute(status->work.get()); }
        catch(BusinessException e) { failureAudit(e.getCode()); throw e; }
        catch(org.springframework.dao.DataIntegrityViolationException e) {failureAudit("DATA_CONFLICT");throw conflict("DATA_CONFLICT","学生重复或关联资料已变化，请刷新草稿");}
        catch(org.springframework.dao.DataAccessException e) {failureAudit("DATABASE_UNAVAILABLE");throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"DATABASE_UNAVAILABLE","学生档案助手数据库暂不可用");}
        catch(RuntimeException e) {failureAudit("INTERNAL_ERROR");throw e;}
    }
    private void failureAudit(String code) {
        Actor current=null;
        try {var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null) current=mapper.actor(auth.getName());}catch(Exception ignored){}
        // 事务已经回滚，独立安全日志不含资料、SQL、密钥或模型推理。
        log.info("Agent rejected traceId={} actorId={} schoolId={} code={}",AgentJson.id(),current==null?null:current.id(),current==null?null:current.schoolId(),code);
    }
    private Actor lockScope() {
        enabled();var a=actor();mapper.lockSchool(a.schoolId());
        var current=mapper.lockActor(SecurityContextHolder.getContext().getAuthentication().getName());
        if(current==null||!"SCHOOL_ADMIN".equals(current.role())||current.schoolId()!=a.schoolId()) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","学生建档权限已撤销");
        return current;
    }
    private Session session(String id,Actor a,boolean requireActive) {
        var s=mapper.lockSession(id,a.schoolId(),a.id());if(s==null) throw BusinessException.notFound("会话");
        if(requireActive&&!s.getExpiresAt().isAfter(AgentJson.now())) throw conflict("SESSION_EXPIRED","会话已过期，请新建会话");
        recover(s);return s;
    }
    private void recover(Session s) {
        if(s.getProcessingMessageId()!=null&&!s.getProcessingUntil().isAfter(AgentJson.now())) {
            var m=mapper.messageById(s.getProcessingMessageId());
            if(m!=null&&"PROCESSING".equals(m.getProcessingStatus())) {
                m.setProcessingStatus("FAILED");m.setUpdatedAt(AgentJson.now());
                m.setResponsePayload(AgentJson.write(Map.of("processingStatus","FAILED","code","PROCESSING_EXPIRED","message","处理已超时，草稿保留；请检查后发送新消息。","traceId",m.getTraceId())));mapper.updateMessage(m);
            }
            s.setProcessingMessageId(null);s.setProcessingUntil(null);s.setUpdatedAt(AgentJson.now());mapper.updateSession(s);
        }
    }
    private void idle(Session s) { if(s.getProcessingMessageId()!=null) throw conflict("REQUEST_PROCESSING","会话正在处理消息，请稍后刷新"); }
    private Draft draft(String id,Actor a) {
        var d=mapper.lockDraft(id,a.schoolId(),a.id());if(d==null) throw BusinessException.notFound("草稿");
        var status=StudentDraftStatus.persisted(d.getStatus());
        if(status.editable()&&!d.getExpiresAt().isAfter(AgentJson.now())) status=StudentDraftStatus.EXPIRED;
        if(!status.name().equals(d.getStatus())) {d.setStatus(status.name());d.setUpdatedAt(AgentJson.now());mapper.updateDraft(d);}
        return d;
    }
    private void editable(Draft d,int version) {
        if(!d.getExpiresAt().isAfter(AgentJson.now())) throw conflict("DRAFT_EXPIRED","草稿已过期，请新建会话");
        if(!List.of("COLLECTING","READY").contains(d.getStatus())) throw conflict("DRAFT_CLOSED","草稿已取消或已提交");
        if(d.getVersion()!=version) throw conflict("VERSION_CONFLICT","草稿已更新，请刷新并重新检查");
    }
    static BusinessException conflict(String code,String message) { return new BusinessException(HttpStatus.CONFLICT,code,message); }
    public Map<String,Object> settings() { var a=actor();return Map.of("enabled",properties.isEnabled(),"modelEnabled",properties.isModelEnabled(),"maxInputLength",properties.getMaxInputLength(),"schoolId",Long.toString(a.schoolId()),"processingMode",properties.getProcessingMode().name().toLowerCase(Locale.ROOT),"requestTimeoutMs",Math.min(135000,(properties.getRequestTimeoutSeconds()+properties.getToolTimeoutSeconds()+10)*1000)); }
    public Map<String,Object> createSession() {
        return transaction(()->{var a=lockScope();var s=new Session();s.setId(AgentJson.id());s.setSchoolId(a.schoolId());s.setOwnerId(a.id());s.setCreatedAt(AgentJson.now());s.setUpdatedAt(s.getCreatedAt());s.setExpiresAt(s.getCreatedAt().plusHours(properties.getDraftTtlHours()));mapper.insertSession(s);
            audit.audit(a.id(),"AGENT_SESSION","school",a.schoolId(),"建立学生建档会话");return Map.of("sessionId",s.getId());});
    }
    public Map<String,Object> viewSession(String id) {
        return transaction(()->{enabled();var a=actor();var s=session(id,a,false);var result=new LinkedHashMap<String,Object>();result.put("sessionId",s.getId());result.put("expired",!s.getExpiresAt().isAfter(AgentJson.now()));result.put("processing",s.getProcessingMessageId()!=null);
            if(s.getActiveDraftId()!=null) result.put("draft",view(draft(s.getActiveDraftId(),a),a));return result;});
    }
    public Map<String,Object> getDraft(String id) { return transaction(()->{enabled();var a=actor();return view(draft(id,a),a);}); }
    Map<String,Object> view(Draft d,Actor a) {
        var fields=AgentJson.object(d.getFieldsJson());var validation=StudentDraftRules.validate(fields,a.schoolId(),mapper);
        String status=d.getStatus();if(List.of("COLLECTING","READY").contains(status)) status=d.getExpiresAt().isAfter(AgentJson.now())?(validation.ready()?"READY":"COLLECTING"):"EXPIRED";
        var result=new LinkedHashMap<String,Object>();result.put("draftId",d.getId());result.put("sessionId",d.getSessionId());result.put("version",d.getVersion());result.put("status",status);result.put("fields",fields);result.put("display",validation.display());
        result.put("missingFields","CREATED".equals(status)?List.of():validation.missingFields());result.put("fieldErrors","CREATED".equals(status)?List.of():validation.fieldErrors());result.put("confirmationRequired",List.of("READY","COLLECTING").contains(status));result.put("sourceMessageIds",AgentJson.read(d.getSourcesJson()));
        if(d.getStudentId()!=null) result.put("studentId",Long.toString(d.getStudentId()));return result;
    }
    public Map<String,Object> patch(String sessionId,String draftId,Integer expectedVersion,JsonNode fields) {
        return transaction(()->{var a=lockScope();var s=session(sessionId,a,true);idle(s);return view(upsert(s,draftId,expectedVersion,fields,null,a),a);});
    }
    Map<String,Object> applyMessage(Message message,String draftId,Integer expectedVersion,JsonNode fields,String departmentName) {
        return transaction(()->{
            var a=lockScope();var s=session(message.getSessionId(),a,true);
            if(!message.getId().equals(s.getProcessingMessageId())) throw conflict("MESSAGE_CLOSED","消息处理已结束，请刷新草稿");
            if(!Objects.equals(s.getActiveDraftId(),draftId)) throw conflict("VERSION_CONFLICT","当前草稿已变化，请刷新后重试");
            Draft current=draftId==null?null:draft(draftId,a);
            if(current!=null) {if(expectedVersion==null)throw BusinessException.badRequest("缺少草稿版本");editable(current,expectedVersion);}
            var normalized=StudentDraftRules.normalize(fields);
            // Only the administrator's actual text can select an ID; extraction DTOs cannot carry IDs.
            var selection=java.util.regex.Pattern.compile("学院(?:编号|\\s*[Ii][Dd])\\s*[:：]?\\s*([1-9][0-9]{0,18})(?![0-9])").matcher(message.getContent());
            String selected=null;
            while(selection.find()) {if(selected!=null)throw conflict("DICTIONARY_AMBIGUOUS","请一次只选择一个学院编号");selected=selection.group(1);}
            if(selected!=null) {
                if(normalized.has("departmentId"))throw BusinessException.badRequest("不能同时选择和清空学院");
                long id;try{id=Long.parseLong(selected);}catch(NumberFormatException error){throw BusinessException.badRequest("学院编号超出范围");}
                String name=mapper.departmentName(id,a.schoolId());
                if(name==null)throw conflict("DICTIONARY_NOT_FOUND","该学院编号不属于可访问的本校学院，请重新选择");
                if(departmentName!=null&&!name.equals(departmentName))throw conflict("DICTIONARY_AMBIGUOUS","学院名称与编号不一致，请明确选择");
                normalized.put("departmentId",selected);
            } else if(departmentName!=null) {
                var candidates=mapper.exactDepartments(a.schoolId(),departmentName);
                if(candidates.isEmpty())throw conflict("DICTIONARY_NOT_FOUND","没有找到名称完全匹配的本校学院，请补充完整学院名称或在表单选择学院编号");
                if(candidates.size()!=1) {
                    String choices=candidates.stream().limit(10).map(row->row.get("name")+"（学院编号 "+row.get("id")+"，代码 "+row.get("code")+"）").collect(java.util.stream.Collectors.joining("；"));
                    throw conflict("DICTIONARY_AMBIGUOUS","本校存在重名学院，请回复“学院编号”及明确编号："+choices);
                }
                normalized.put("departmentId",candidates.get(0).get("id").toString());
            }
            if(normalized.isEmpty())return current==null?null:view(current,a);
            return view(upsert(s,draftId,expectedVersion,normalized,message.getId(),a),a);
        });
    }
    private Draft upsert(Session s,String id,Integer version,JsonNode patch,String source,Actor a) {
        ObjectNode normalized=StudentDraftRules.normalize(patch);Draft d;
        if(id==null) {
            if(s.getActiveDraftId()!=null) {
                var current=draft(s.getActiveDraftId(),a);
                if(current.getExpiresAt().isAfter(AgentJson.now())&&List.of("COLLECTING","READY").contains(current.getStatus())) throw conflict("ACTIVE_DRAFT","请更新当前草稿或先取消");
            }
            d=new Draft();d.setId(AgentJson.id());d.setSessionId(s.getId());d.setSchoolId(a.schoolId());d.setOwnerId(a.id());d.setVersion(1);d.setStatus("COLLECTING");d.setFieldsJson("{}");d.setSourcesJson("{}");d.setCreatedAt(AgentJson.now());d.setUpdatedAt(d.getCreatedAt());d.setExpiresAt(s.getExpiresAt());
        } else {
            d=draft(id,a);if(!s.getId().equals(d.getSessionId())||!id.equals(s.getActiveDraftId())) throw BusinessException.notFound("本会话草稿");
            if(version==null) throw BusinessException.badRequest("更新必须携带 expectedVersion");editable(d,version);d.setVersion(d.getVersion()+1);
        }
        var fields=AgentJson.object(d.getFieldsJson());var sources=AgentJson.object(d.getSourcesJson());
        normalized.fields().forEachRemaining(entry->{fields.set(entry.getKey(),entry.getValue());sources.put(entry.getKey(),source==null?"MANUAL":source);});
        var validation=StudentDraftRules.validate(fields,a.schoolId(),mapper);
        // 合法 ID 也不能跨学校，拒绝整次更新，不保留越界字段。
        if(fields.hasNonNull("departmentId")&&mapper.departmentName(Long.parseLong(fields.path("departmentId").asText()),a.schoolId())==null) throw BusinessException.notFound("本校学院");
        d.setFieldsJson(AgentJson.write(fields));d.setSourcesJson(AgentJson.write(sources));d.setStatus(validation.ready()?"READY":"COLLECTING");d.setUpdatedAt(AgentJson.now());
        if(id==null) {mapper.insertDraft(d);s.setActiveDraftId(d.getId());s.setUpdatedAt(AgentJson.now());mapper.updateSession(s);} else mapper.updateDraft(d);
        audit.audit(a.id(),"AGENT_DRAFT","school",a.schoolId(),"草稿 "+d.getId()+" v"+d.getVersion());return d;
    }
    public Map<String,Object> cancel(String id,int version) {
        return transaction(()->{var a=lockScope();var initial=draft(id,a);var s=session(initial.getSessionId(),a,true);idle(s);var d=draft(id,a);editable(d,version);d.setStatus("CANCELLED");d.setUpdatedAt(AgentJson.now());mapper.updateDraft(d);audit.audit(a.id(),"AGENT_CANCEL","school",a.schoolId(),"草稿 "+id+" v"+version);return view(d,a);});
    }
    public Map<String,Object> confirm(String id,int version,String key) {
        if(key==null||!key.matches("[A-Za-z0-9_-]{8,80}")) throw BusinessException.badRequest("幂等键须为 8 至 80 位字母、数字、下划线或短横线");
        String hash=AgentJson.hash("CREATE_STUDENT_PROFILE:"+id+":"+version);
        return transaction(()->{var a=lockScope();var d=draft(id,a);var saved=mapper.action(a.schoolId(),a.id(),key);
            if(saved!=null) {if(!hash.equals(saved.getRequestHash())) throw conflict("IDEMPOTENCY_CONFLICT","同一幂等键对应不同草稿或版本");if(mapper.studentAccessible(saved.getStudentId(),a.schoolId())!=1) throw BusinessException.notFound("学生档案");return replay(saved.getResultJson());}
            if("CREATED".equals(d.getStatus())) {
                if(!Objects.equals(d.getConfirmedVersion(),version)) throw conflict("VERSION_CONFLICT","确认版本与已提交版本不同");
                if(mapper.studentAccessible(d.getStudentId(),a.schoolId())!=1) throw BusinessException.notFound("学生档案");
                var result=committed(d,true);saveAction(a,d,version,key,hash,result);return result;
            }
            var s=session(d.getSessionId(),a,true);idle(s);editable(d,version);var fields=AgentJson.object(d.getFieldsJson());
            if(!StudentDraftRules.validate(fields,a.schoolId(),mapper).ready()) throw conflict("DRAFT_INVALID","草稿未通过校验，请检查必填项、学院和重复学号");
            long studentId=creation.create(fields);
            d.setStatus("CREATED");d.setStudentId(studentId);d.setConfirmedBy(a.id());d.setConfirmedAt(AgentJson.now());d.setConfirmedVersion(version);d.setConfirmedPayloadHash(AgentJson.hash(AgentJson.write(fields)));d.setUpdatedAt(AgentJson.now());mapper.updateDraft(d);
            var result=committed(d,false);saveAction(a,d,version,key,hash,result);audit.audit(a.id(),"AGENT_CREATE_PROFILE","student_profile",studentId,"草稿 "+id+" v"+version+" 已确认；仅建档");return result;});
    }
    private Map<String,Object> committed(Draft d,boolean replayed) {return Map.of("draftId",d.getId(),"status","CREATED","studentId",Long.toString(d.getStudentId()),"created",true,"replayed",replayed,"message","学生档案已创建");}
    private Map<String,Object> replay(String payload) {var result=AgentJson.JSON.convertValue(AgentJson.read(payload),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});if("COMMITTED".equals(result.get("status"))) result.put("status","CREATED");result.put("replayed",true);return result;}
    private void saveAction(Actor a,Draft d,int version,String key,String hash,Map<String,Object> result) {var action=new Action();action.setId(AgentJson.id());action.setSchoolId(a.schoolId());action.setActorId(a.id());action.setActionType("CREATE_STUDENT_PROFILE");action.setIdempotencyKey(key);action.setDraftId(d.getId());action.setDraftVersion(version);action.setRequestHash(hash);action.setStudentId(d.getStudentId());action.setResultJson(AgentJson.write(result));action.setCreatedAt(AgentJson.now());mapper.insertAction(action);}

    record Started(Message message,Map<String,Object> response,Map<String,Object> context) {}
    Started start(String sessionId,String clientId,String content) {
        if(clientId==null||!clientId.matches("[A-Za-z0-9_-]{1,80}")) throw BusinessException.badRequest("clientMessageId 格式不合法");
        if(content==null||content.isBlank()||content.length()>properties.getMaxInputLength()) throw BusinessException.badRequest("请输入长度限制以内的学生资料");
        return transaction(()->{var a=lockScope();var s=session(sessionId,a,true);var old=mapper.message(sessionId,clientId);String hash=AgentJson.hash(content);
            if(old!=null) {if(!hash.equals(old.getRequestHash())) throw conflict("MESSAGE_CONFLICT","同一消息 ID 不能用于不同内容");return new Started(old,messageView(old),Map.of());}
            idle(s);if(mapper.recentMessages(a.id(),a.schoolId())>=properties.getRequestsPerMinute()) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMITED","消息过于频繁，请稍后重试");
            var m=new Message();m.setId(AgentJson.id());m.setSessionId(sessionId);m.setClientMessageId(clientId);m.setRequestHash(hash);m.setContent(content);m.setTraceId(AgentJson.id());m.setProcessingStatus("PROCESSING");m.setCreatedAt(AgentJson.now());m.setUpdatedAt(m.getCreatedAt());mapper.insertMessage(m);
            s.setProcessingMessageId(m.getId());s.setProcessingUntil(AgentJson.now().plusSeconds(properties.getRequestTimeoutSeconds()+properties.getToolTimeoutSeconds()+5));s.setUpdatedAt(AgentJson.now());mapper.updateSession(s);
            var context=new LinkedHashMap<String,Object>();if(s.getActiveDraftId()!=null) context.put("draft",view(draft(s.getActiveDraftId(),a),a));return new Started(m,null,context);});
    }
    private Map<String,Object> messageView(Message m) {
        if(m.getResponsePayload()!=null) return AgentJson.JSON.convertValue(AgentJson.read(m.getResponsePayload()),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
        return Map.of("processingStatus",m.getProcessingStatus(),"clientMessageId",m.getClientMessageId(),"traceId",m.getTraceId(),"message","PROCESSING".equals(m.getProcessingStatus())?"消息正在处理，请稍后查询":"历史资料已按保留期清理");
    }
    public List<Map<String,Object>> history(String sessionId,int page,int size) {
        var q=new PageQuery(page,size);return transaction(()->{enabled();var a=actor();session(sessionId,a,false);return mapper.messages(sessionId,q.size(),q.offset()).stream().map(m->{var row=new LinkedHashMap<String,Object>();row.put("clientMessageId",m.getClientMessageId());row.put("content",m.getContent());row.put("result",messageView(m));return (Map<String,Object>)row;}).toList();});
    }
    Map<String,Object> finish(Message m,String text,String error) {
        return transaction(()->{var a=lockScope();var s=session(m.getSessionId(),a,false);
            if(!m.getId().equals(s.getProcessingMessageId())) return messageView(mapper.messageById(m.getId()));
            var response=new LinkedHashMap<String,Object>();response.put("processingStatus",error==null?"SUCCEEDED":"FAILED");response.put("message",text);response.put("traceId",m.getTraceId());response.put("clientMessageId",m.getClientMessageId());if(error!=null) response.put("code",error);
            if(s.getActiveDraftId()!=null) response.put("draft",view(draft(s.getActiveDraftId(),a),a));
            m.setProcessingStatus(error==null?"SUCCEEDED":"FAILED");m.setResponsePayload(AgentJson.write(response));m.setUpdatedAt(AgentJson.now());mapper.updateMessage(m);s.setProcessingMessageId(null);s.setProcessingUntil(null);s.setUpdatedAt(AgentJson.now());mapper.updateSession(s);
            log.info("Agent message traceId={} result={}",m.getTraceId(),error==null?"OK":error);return response;});
    }
    JsonNode tool(Message m,String callId,String name,ObjectNode arguments) {
        long started=System.nanoTime();
        if(callId==null||!callId.matches("[A-Za-z0-9_-]{1,80}")) throw BusinessException.badRequest("工具调用 ID 不合法");
        return transaction(()->{var a=lockScope();var s=session(m.getSessionId(),a,true);
            if(!m.getId().equals(s.getProcessingMessageId())) throw conflict("MESSAGE_CLOSED","消息处理已结束");
            String hash=AgentJson.hash(name+":"+AgentJson.write(arguments));var previous=mapper.toolCall(m.getId(),callId);
            if(previous!=null) {if(!hash.equals(previous.getRequestHash())) throw conflict("TOOL_CONFLICT","工具调用 ID 载荷冲突");return AgentJson.read(previous.getResultJson());}
            Object data=switch(name) {
                case "search_departments" -> {AgentJson.keys(arguments,Set.of("keyword"));yield mapper.departments(a.schoolId(),AgentJson.text(arguments.get("keyword"),100));}
                case "check_student_duplicate" -> {AgentJson.keys(arguments,Set.of("studentNo"));yield Map.of("duplicate",mapper.duplicates(a.schoolId(),AgentJson.text(arguments.get("studentNo"),40))>0);}
                case "get_student_draft" -> {AgentJson.keys(arguments,Set.of("draftId"));var d=draft(AgentJson.text(arguments.get("draftId"),36),a);if(!d.getSessionId().equals(s.getId())) throw BusinessException.notFound("本会话草稿");yield view(d,a);}
                case "upsert_student_draft" -> {
                    AgentJson.keys(arguments,Set.of("draftId","expectedVersion","fields","studentCount"));
                    if(!arguments.path("studentCount").isIntegralNumber()||!arguments.path("studentCount").canConvertToInt()||arguments.path("studentCount").asInt()!=1) throw BusinessException.badRequest("一次只能处理一名学生，请逐人提供资料");
                    String id=arguments.hasNonNull("draftId")?AgentJson.text(arguments.get("draftId"),36):null;
                    Integer version=arguments.hasNonNull("expectedVersion")?AgentJson.version(arguments.get("expectedVersion")):null;
                    dictionarySelection(s,m,arguments.get("fields"),a);
                    yield view(upsert(s,id,version,arguments.get("fields"),m.getId(),a),a);
                }
                default -> throw BusinessException.badRequest("模型请求了未允许的工具");
            };
            var result=AgentJson.JSON.valueToTree(Map.of("ok",true,"data",data,"traceId",m.getTraceId()));var call=new ToolCall();call.setMessageId(m.getId());call.setCallId(callId);call.setRequestHash(hash);call.setResultJson(AgentJson.write(result));call.setCreatedAt(AgentJson.now());mapper.insertToolCall(call);
            log.info("Agent tool traceId={} tool={} elapsedMs={} result=OK",m.getTraceId(),name,(System.nanoTime()-started)/1000000);return result;});
    }
    private void dictionarySelection(Session session,Message message,JsonNode fields,Actor actor) {
        var normalized=StudentDraftRules.normalize(fields);if(!normalized.hasNonNull("departmentId")) return;
        String id=normalized.path("departmentId").asText();
        if(session.getActiveDraftId()!=null&&id.equals(AgentJson.object(draft(session.getActiveDraftId(),actor).getFieldsJson()).path("departmentId").asText())) return;
        if(java.util.regex.Pattern.compile("学院(?:编号|\\s*[Ii][Dd])?\\s*[:：]?\\s*"+java.util.regex.Pattern.quote(id)+"(?![0-9])").matcher(message.getContent()).find()) return;
        for(String saved:mapper.toolResults(message.getId())) {
            var candidates=AgentJson.read(saved).path("data");
            if(candidates.isArray()&&candidates.size()==1&&id.equals(candidates.get(0).path("id").asText())) return;
        }
        throw conflict("DICTIONARY_AMBIGUOUS","学院未明确选择或存在重名，请回复“学院编号”及候选 ID 后重试");
    }
}
