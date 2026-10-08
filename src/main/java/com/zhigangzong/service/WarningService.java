package com.zhigangzong.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.WarningRequests.Policy;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class WarningService {
 private final PortalService portal; private final PortalMapper accounts; private final WarningMapper mapper; private final CaseMapper cases; private final ManagementMapper audit; private final ObjectMapper json;
 private String write(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}}
 private Policy read(String s){try{return json.readValue(s,Policy.class);}catch(JsonProcessingException ex){throw BusinessException.badRequest("预警规则无效，请重新配置");}}
 private InternshipBatch batch(long id,PortalMapper.Actor a){var b=accounts.batch(id);if(b==null || accountsDepartmentSchool(b.getDepartmentId())!=a.schoolId())throw BusinessException.notFound("本校批次");return b;}
 private long accountsDepartmentSchool(long id){return mapper.departmentSchool(id);}
 public List<Map<String,Object>> batches(){return mapper.batches(portal.actor("SCHOOL_ADMIN").schoolId());}
 public Map<String,Object> settings(long id){var a=portal.actor("SCHOOL_ADMIN");batch(id,a);var result=new LinkedHashMap<String,Object>();String s=mapper.policy(id);if(s!=null)result.put("policy",read(s));result.put("events",mapper.events(id));result.put("scans",mapper.scans(id));return result;}
 public Map<String,Object> save(long id,Policy r){var a=portal.actor("SCHOOL_ADMIN");batch(id,a);if(new HashSet<>(r.requiredKinds()).size()!=r.requiredKinds().size() || (r.materials()&&r.requiredKinds().isEmpty()))throw BusinessException.badRequest("请配置不重复的必需材料类型");mapper.save(id,write(r));mapper.policyEvent(id,a.id(),write(r));audit.audit(a.id(),"WARNING_POLICY","internship_batch",id,"保存批次预警规则");return settings(id);}
 public Map<String,Object> contacts(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");return Map.of("placement",portal.accessiblePlacement(id,a),"contacts",mapper.contacts(id));}
 public Map<String,Object> contact(long id,String note){var a=portal.actor("TEACHER","ENTERPRISE_MENTOR");var p=portal.accessiblePlacement(id,a);portal.requireCurrent(p);if(!"APPROVED".equals(p.getSchoolApprovalStatus()))throw BusinessException.badRequest("学校批准后才能记录联系");mapper.contact(id,a.id(),LocalDateTime.now(ZoneId.of("Asia/Shanghai")),note);audit.audit(a.id(),"GUIDANCE_CONTACT","internship_placement",id,note);return contacts(id);}
 public Map<String,Object> scan(long id){
  var a=portal.actor("SCHOOL_ADMIN");var b=batch(id,a);String settings=mapper.policy(id);if(settings==null)throw BusinessException.badRequest("请先配置批次预警规则");var r=read(settings);var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
  if(today.isBefore(b.getStartDate()))throw BusinessException.badRequest("批次尚未开始");
  var previous=new HashMap<String,Map<String,Object>>();for(var d:mapper.detections(id))previous.put((String)d.get("detection_key"),d);var triggered=new HashSet<String>();var created=new ArrayList<Long>();var students=mapper.students(a.schoolId(),b.getDepartmentId());
  for(long student:students){accounts.lockStudent(student);InternshipPlacement p=null;for(long pid:mapper.placements(student,id)){var candidate=accounts.lockPlacement(pid);if("APPROVED".equals(candidate.getSchoolApprovalStatus())&&candidate.getReplacementPlacementId()==null&&candidate.getTerminationRequestId()==null){p=candidate;break;}}
   if(p==null){if(r.unplaced()&&!today.isBefore(b.getStartDate().plusDays(r.unplacedGraceDays())))detect(a,id,student,null,"UNPLACED","尚未落实获批单位",Map.of("deadline",b.getStartDate().plusDays(r.unplacedGraceDays())),previous,triggered,created);continue;}
   if("APPROVED".equals(p.getArchiveStatus()))continue;
   if(r.materials()&&today.isAfter(p.getStartDate().plusDays(r.materialDays()))){var missing=new ArrayList<String>();for(String kind:r.requiredKinds())if(mapper.material(p.getId(),kind)==0)missing.add(kind);if(!missing.isEmpty())detect(a,id,student,p.getId(),"MATERIAL","必需材料逾期或未通过",Map.of("missingKinds",missing,"deadline",p.getStartDate().plusDays(r.materialDays())),previous,triggered,created);}
   if(r.reports()&&"ARRIVED".equals(p.getArrivalStatus())){var reports=mapper.reports(p.getId()).stream().filter(v->!v.getPeriodEnd().isAfter(today)).toList();for(var start=p.getStartDate();!start.isAfter(p.getEndDate());start=start.plusDays(r.frequencyDays())){var end=start.plusDays(r.frequencyDays()-1);if(end.isAfter(p.getEndDate()))end=p.getEndDate();if(!today.isAfter(end.plusDays(r.reportGraceDays())))break;var from=start;var to=end;if(reports.stream().noneMatch(v->!v.getPeriodStart().isAfter(from)&&!v.getPeriodEnd().isBefore(to)))detect(a,id,student,p.getId(),"REPORT:"+start,"周报缺交",Map.of("periodStart",start,"periodEnd",end,"deadline",end.plusDays(r.reportGraceDays())),previous,triggered,created);}}
   if(r.contact()&&"ARRIVED".equals(p.getArrivalStatus())&&!today.isAfter(p.getEndDate())){var last=mapper.lastContact(p.getId());var date=last==null?p.getStartDate():last.toLocalDate();if(ChronoUnit.DAYS.between(date,today)>=r.contactDays())detect(a,id,student,p.getId(),"CONTACT","长期未记录指导联系",Map.of("lastContact",date,"thresholdDays",r.contactDays()),previous,triggered,created);}
  }
  for(String key:previous.keySet())if(!triggered.contains(key))mapper.clear(key);mapper.scanEvent(id,a.id(),students.size(),created.size(),settings);return Map.of("checkedStudents",students.size(),"createdCaseIds",created);
 }
 private void detect(PortalMapper.Actor a,long batch,long student,Long placement,String type,String title,Object evidence,Map<String,Map<String,Object>> previous,Set<String> triggered,List<Long> created){
  String key=batch+":"+student+":"+type;triggered.add(key);var old=previous.get(key);if(old!=null&&(Boolean.TRUE.equals(old.get("active")) || (old.get("active") instanceof Number n && n.intValue()==1)))return;int episode=old==null?1:((Number)old.get("episode")).intValue()+1;
  var c=new StudentCase();c.setSchoolId(a.schoolId());c.setStudentId(student);c.setPlacementId(placement);c.setKind("WARNING");c.setTitle(title);c.setDescription("批次规则检测产生，请核查实际情况后分派、跟进、提交结果及复核。");c.setRuleKey(key+":"+episode);c.setEvidence(write(evidence));cases.create(c);c=cases.lock(c.getId());cases.event(c.getId(),a.id(),"DETECTED",c.getDescription(),write(c));cases.notifySchool(a.schoolId(),c.getId());mapper.detected(key,batch,episode,c.getId());created.add(c.getId());accounts.notifyUser(accounts.lockStudent(student).getUserId(),"实习规则预警",title,"CASE",c.getId());
 }
}
