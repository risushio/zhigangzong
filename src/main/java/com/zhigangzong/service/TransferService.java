package com.zhigangzong.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.TransferRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class TransferService {
    private final PortalService portal;
    private final PortalMapper accounts;
    private final TransferMapper mapper;
    private final ExtensionMapper history;
    private final SelfPlacementMapper self;
    private final ManagementMapper management;
    private final StudentProfileMapper students;
    private final ObjectMapper json;

    private String snapshot(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}}
    private <T> T read(String value,Class<T> type){try{return json.readValue(value,type);}catch(JsonProcessingException ex){throw BusinessException.badRequest("变更快照无效，请联系学校核查");}}
    private Original original(InternshipPlacement p){return new Original(p.getEnterpriseId(),management.lockEnterprise(p.getEnterpriseId()).getName(),p.getJobId(),p.getPositionTitle(),p.getSchoolApprovalStatus(),p.getTeacherId(),p.getEnterpriseMentorId(),p.getStartDate(),p.getEndDate(),p.getTerminationRequestId());}
    private void eligible(InternshipPlacement p){portal.requireNotReplaced(p);if(!List.of("APPROVED","REJECTED").contains(p.getSchoolApprovalStatus()))throw BusinessException.badRequest("仅已批准或已拒绝的实习可以申请重新安排");}
    private void unchanged(ChangeRequest r,InternshipPlacement p){
        // Names may be corrected independently; identity, dates, decision and mentor relationships must still match.
        var o=read(r.getOriginalSnapshot(),Original.class);
        if(!Objects.equals(o.terminationRequestId(),p.getTerminationRequestId()))throw BusinessException.badRequest("原实习终止状态已变化，请重新申请");
        if(!Objects.equals(o.enterpriseId(),p.getEnterpriseId()) || !Objects.equals(o.jobId(),p.getJobId()) || !Objects.equals(o.positionTitle(),p.getPositionTitle()) || !Objects.equals(o.approvalStatus(),p.getSchoolApprovalStatus()) || !Objects.equals(o.teacherId(),p.getTeacherId()) || !Objects.equals(o.enterpriseMentorId(),p.getEnterpriseMentorId()) || !Objects.equals(o.startDate(),p.getStartDate()) || !Objects.equals(o.endDate(),p.getEndDate()))throw BusinessException.badRequest("原实习安排已变化，请拒绝旧申请后重新申请");
    }
    private Target target(InternshipPlacement p,Apply r){
        var b=accounts.batch(p.getBatchId());
        if(r.endDate().isBefore(r.startDate()) || r.startDate().isBefore(b.getStartDate()) || r.endDate().isAfter(b.getEndDate()))throw BusinessException.badRequest("新实习日期必须有序且在原批次范围内");
        if("PLATFORM".equals(r.source())){
            if(r.applicationId()==null || r.self()!=null)throw BusinessException.badRequest("请选择已确认录用的申请，平台安排不能携带自主单位资料");
            var a=accounts.lockApplication(r.applicationId());
            if(a==null || !Objects.equals(a.getStudentId(),p.getStudentId()))throw BusinessException.notFound("本人录用申请");
            if(!"ACCEPTED".equals(a.getRecruitmentStatus()) || mapper.usedApplication(a.getId())>0)throw BusinessException.badRequest("新申请须已确认录用且尚未用于实习安排");
            var j=management.lockJob(a.getJobId());var e=management.lockEnterprise(j.getEnterpriseId());
            if(!"APPROVED".equals(e.getReviewStatus()) || !"APPROVED".equals(j.getReviewStatus()))throw BusinessException.badRequest("目标单位和岗位须已通过审核");
            if((j.getStartDate()!=null && r.startDate().isBefore(j.getStartDate())) || (j.getEndDate()!=null && r.endDate().isAfter(j.getEndDate())))throw BusinessException.badRequest("新实习日期须在岗位实习期间内");
            if(p.getTerminationRequestId()==null && Objects.equals(j.getId(),p.getJobId()))throw BusinessException.badRequest("请选择不同岗位或单位");
            return new Target("PLATFORM",a.getId(),e.getId(),e.getName(),j.getId(),j.getTitle(),r.startDate(),r.endDate(),null);
        }
        var s=r.self();
        if(s==null || r.applicationId()!=null || !Objects.equals(s.batchId(),p.getBatchId()) || !s.startDate().equals(r.startDate()) || !s.endDate().equals(r.endDate()))throw BusinessException.badRequest("请填写原批次的完整自主单位资料及一致的日期");
        var e=self.enterprise(s.creditCode());
        if(e!=null && (!e.getName().equals(s.enterpriseName().trim()) || List.of("SUSPENDED","REJECTED").contains(e.getReviewStatus())))throw BusinessException.badRequest("单位名称不符、已暂停或未通过审核，请核对单位");
        if(p.getTerminationRequestId()==null && e!=null && Objects.equals(e.getId(),p.getEnterpriseId()) && s.positionTitle().trim().equals(p.getPositionTitle()))throw BusinessException.badRequest("请选择不同岗位或单位");
        return new Target("SELF",null,e==null?null:e.getId(),s.enterpriseName().trim(),null,s.positionTitle().trim(),r.startDate(),r.endDate(),s);
    }
    private String changeType(InternshipPlacement p,Target t){return Objects.equals(p.getEnterpriseId(),t.enterpriseId())?"CHANGE_JOB":"CHANGE_ENTERPRISE";}
    public Map<String,Object> list(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=portal.accessiblePlacement(id,a);return Map.of("placement",p,"requests",mapper.list(id));}
    private ChangeRequest accessible(long id,PortalMapper.Actor a){var r=mapper.lookup(id);if(r==null)throw BusinessException.notFound("换岗或换单位申请");portal.accessiblePlacement(r.getPlacementId(),a);return mapper.lock(id);}
    public Map<String,Object> detail(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var r=accessible(id,a);var result=new LinkedHashMap<String,Object>();result.put("request",r);result.put("events",history.events(id));var next=mapper.replacement(id);if(next!=null && List.of("STUDENT","SCHOOL_ADMIN").contains(a.role()))result.put("replacementPlacementId",next);return result;}
    public ChangeRequest apply(long id,Apply request){
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);eligible(p);
        if(!history.active(id).isEmpty())throw BusinessException.badRequest("已有待审或退回的变更申请，请先处理原申请");
        var t=target(p,request);var r=new ChangeRequest();r.setPlacementId(id);r.setChangeType(changeType(p,t));r.setOriginalSnapshot(snapshot(original(p)));r.setRequestedSnapshot(snapshot(t));r.setReason(request.reason());r.setStatus("PENDING");
        mapper.create(r);history.event(r,a.id());mapper.notifySchool(a.schoolId(),id);management.audit(a.id(),"TRANSFER_APPLY","change_request",r.getId(),r.getReason());return mapper.lock(r.getId());
    }
    public ChangeRequest resubmit(long id,Apply request){
        var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);eligible(p);
        if(!"RETURNED".equals(r.getStatus()))throw BusinessException.badRequest("仅退回的变更申请可补充重提");unchanged(r,p);
        var t=target(p,request);r.setChangeType(changeType(p,t));r.setRequestedSnapshot(snapshot(t));r.setReason(request.reason());r.setStatus("PENDING");r.setReviewerId(null);r.setReviewComment(null);
        mapper.save(r);history.event(r,a.id());mapper.notifySchool(a.schoolId(),p.getId());management.audit(a.id(),"TRANSFER_RESUBMIT","change_request",id,r.getReason());return r;
    }
    public Map<String,Object> review(long id,Review request){
        var a=portal.actor("SCHOOL_ADMIN");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);portal.requireNotReplaced(p);
        if(!"PENDING".equals(r.getStatus()))throw BusinessException.badRequest("仅待审的变更申请可处理");
        Long next=null;
        if("APPROVED".equals(request.decision())){
            eligible(p);unchanged(r,p);var saved=read(r.getRequestedSnapshot(),Target.class);
            var t=target(p,new Apply(saved.source(),saved.applicationId(),saved.self(),saved.startDate(),saved.endDate(),r.getReason()));
            if(!t.equals(saved))throw BusinessException.badRequest("目标单位或岗位资料已变化，请退回学生核对后重提");
            var n=new InternshipPlacement();n.setStudentId(p.getStudentId());n.setBatchId(p.getBatchId());n.setEnterpriseId(t.enterpriseId());n.setApplicationId(t.applicationId());n.setJobId(t.jobId());n.setPositionTitle(t.positionTitle());n.setStartDate(t.startDate());n.setEndDate(t.endDate());
            if("PLATFORM".equals(t.source()))accounts.placement(n);
            else {
                if(n.getEnterpriseId()==null){var e=new Enterprise();e.setName(t.enterpriseName());e.setCreditCode(t.self().creditCode());e.setContactName(t.self().contactName());e.setContactPhone(t.self().contactPhone());self.createEnterprise(e);n.setEnterpriseId(e.getId());}
                self.create(n);self.insertDetail(n.getId(),t.self());self.event(n.getId(),a.id(),"SUBMITTED");
            }
            next=n.getId();accounts.approval(next,"APPROVED");accounts.approvalRecord(next,a.id(),"APPROVED",request.comment());mapper.link(p.getId(),next,id);
        }
        r.setStatus(request.decision());r.setReviewerId(a.id());r.setReviewComment(request.comment());mapper.save(r);history.event(r,a.id());management.audit(a.id(),"TRANSFER_REVIEW","change_request",id,r.getStatus()+"："+request.comment());
        var recipients=new HashSet<Long>();recipients.add(students.findById(p.getStudentId()).getUserId());if(p.getTeacherId()!=null)recipients.add(p.getTeacherId());if(p.getEnterpriseMentorId()!=null)recipients.add(p.getEnterpriseMentorId());
        for(long user:recipients)accounts.notifyUser(user,"换岗或换单位审批结果",request.decision()+"："+request.comment(),"PLACEMENT",p.getId());
        var result=new LinkedHashMap<String,Object>();result.put("request",r);if(next!=null)result.put("replacementPlacementId",next);return result;
    }
}
