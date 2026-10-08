package com.zhigangzong.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.TerminationRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class TerminationService {
    private final PortalService portal;
    private final PortalMapper accounts;
    private final TerminationMapper mapper;
    private final ExtensionMapper history;
    private final ManagementMapper audit;
    private final StudentProfileMapper students;
    private final ObjectMapper json;

    private Arrangement arrangement(InternshipPlacement p){return new Arrangement(p.getEnterpriseId(),p.getJobId(),p.getPositionTitle(),p.getSchoolApprovalStatus(),p.getTeacherId(),p.getEnterpriseMentorId(),p.getStartDate(),p.getEndDate());}
    private String snapshot(Arrangement p){try{return json.writeValueAsString(p);}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}}
    private void unchanged(ChangeRequest r,InternshipPlacement p){
        try{if(!arrangement(p).equals(json.readValue(r.getOriginalSnapshot(),Arrangement.class)))throw BusinessException.badRequest("原实习安排已变化，请拒绝旧终止申请后重新申请");}
        catch(JsonProcessingException ex){throw BusinessException.badRequest("原实习快照无效，请联系学校核查");}
    }
    private void eligible(InternshipPlacement p){portal.requireCurrent(p);if(!"APPROVED".equals(p.getSchoolApprovalStatus()))throw BusinessException.badRequest("仅学校已批准的实习可以申请终止");}
    public Map<String,Object> list(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");return Map.of("placement",portal.accessiblePlacement(id,a),"requests",mapper.list(id));}
    private ChangeRequest accessible(long id,PortalMapper.Actor a){var r=mapper.lookup(id);if(r==null)throw BusinessException.notFound("终止申请");portal.accessiblePlacement(r.getPlacementId(),a);return mapper.lock(id);}
    public Map<String,Object> detail(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");return Map.of("request",accessible(id,a),"events",history.events(id));}
    public ChangeRequest apply(long id,Apply request){
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);eligible(p);
        if(!history.active(id).isEmpty())throw BusinessException.badRequest("已有待审或退回的变更申请，请先处理原申请");
        var r=new ChangeRequest();r.setPlacementId(id);r.setChangeType("TERMINATE");r.setStatus("PENDING");r.setOriginalSnapshot(snapshot(arrangement(p)));r.setRequestedSnapshot("{\"effect\":\"ON_APPROVAL\"}");r.setReason(request.reason());
        mapper.create(r);history.event(r,a.id());mapper.notifySchool(a.schoolId(),id);audit.audit(a.id(),"TERMINATION_APPLY","change_request",r.getId(),r.getReason());return mapper.lock(r.getId());
    }
    public ChangeRequest resubmit(long id,Apply request){
        var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);eligible(p);
        if(!"RETURNED".equals(r.getStatus()))throw BusinessException.badRequest("仅退回的终止申请可补充重提");unchanged(r,p);
        r.setReason(request.reason());r.setStatus("PENDING");r.setReviewerId(null);r.setReviewComment(null);history.save(r);history.event(r,a.id());mapper.notifySchool(a.schoolId(),p.getId());audit.audit(a.id(),"TERMINATION_RESUBMIT","change_request",id,r.getReason());return r;
    }
    public ChangeRequest review(long id,Review request){
        var a=portal.actor("SCHOOL_ADMIN");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);portal.requireCurrent(p);
        if(!"PENDING".equals(r.getStatus()) && !("RETURNED".equals(r.getStatus()) && "REJECTED".equals(request.decision())))throw BusinessException.badRequest("仅待审申请可审批，退回申请仅可拒绝关闭");
        if("APPROVED".equals(request.decision())){eligible(p);unchanged(r,p);mapper.terminate(p.getId(),id);}
        r.setStatus(request.decision());r.setReviewerId(a.id());r.setReviewComment(request.comment());history.save(r);history.event(r,a.id());audit.audit(a.id(),"TERMINATION_REVIEW","change_request",id,r.getStatus()+"："+request.comment());
        var recipients=new HashSet<Long>();recipients.add(students.findById(p.getStudentId()).getUserId());if(p.getTeacherId()!=null)recipients.add(p.getTeacherId());if(p.getEnterpriseMentorId()!=null)recipients.add(p.getEnterpriseMentorId());
        for(long user:recipients)accounts.notifyUser(user,"实习终止审批结果",request.decision()+"："+request.comment(),"PLACEMENT",p.getId());return r;
    }
}
