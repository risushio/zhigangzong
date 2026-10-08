package com.zhigangzong.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.ExtensionRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class ExtensionService {
    private final PortalService portal;
    private final PortalMapper accounts;
    private final ExtensionMapper mapper;
    private final StudentProfileMapper students;
    private final ManagementMapper audit;
    private final ObjectMapper json;

    private String snapshot(Dates dates) {
        try{return json.writeValueAsString(dates);}catch(JsonProcessingException ex){throw new IllegalStateException("Cannot serialize extension dates",ex);}
    }
    private Dates dates(String value) {
        try{var d=json.readValue(value,Dates.class);if(d==null || d.startDate()==null || d.endDate()==null)throw new IllegalArgumentException();return d;}
        catch(JsonProcessingException | IllegalArgumentException ex){throw BusinessException.badRequest("延期日期快照无效，请联系管理员核查原记录");}
    }
    private void approved(InternshipPlacement p) {
        portal.requireCurrent(p);
        if(!"APPROVED".equals(p.getSchoolApprovalStatus()))throw BusinessException.badRequest("学校批准后的实习才能申请延期");
    }
    private void validate(LocalDate original,LocalDate requested) {
        if(!requested.isAfter(original) || requested.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai"))))
            throw BusinessException.badRequest("延期结束日期必须晚于原结束日期且不能早于今天");
    }
    private void unchanged(ChangeRequest r,InternshipPlacement p) {
        if(!dates(r.getOriginalSnapshot()).equals(new Dates(p.getStartDate(),p.getEndDate())))
            throw BusinessException.badRequest("实习日期已变化，原延期申请不能继续执行，请联系学校管理员核查");
    }
    public Map<String,Object> list(long id) {
        var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=portal.accessiblePlacement(id,a);
        return Map.of("placement",p,"requests",mapper.list(id));
    }
    public ChangeRequest apply(long id,Apply request) {
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);approved(p);validate(p.getEndDate(),request.endDate());
        if(!mapper.active(id).isEmpty())throw BusinessException.badRequest("实习已有待审或退回的变更申请，请先处理原申请");
        var r=new ChangeRequest();r.setPlacementId(id);r.setChangeType("EXTEND");r.setStatus("PENDING");
        r.setOriginalSnapshot(snapshot(new Dates(p.getStartDate(),p.getEndDate())));r.setRequestedSnapshot(snapshot(new Dates(p.getStartDate(),request.endDate())));r.setReason(request.reason());
        mapper.create(r);mapper.event(r,a.id());audit.audit(a.id(),"EXTENSION_APPLY","change_request",r.getId(),r.getReason());
        mapper.notifySchool(a.schoolId(),id);return mapper.lock(r.getId());
    }
    private ChangeRequest accessible(long id,PortalMapper.Actor a) {
        var r=mapper.lookup(id);if(r==null)throw BusinessException.notFound("延期申请");
        portal.accessiblePlacement(r.getPlacementId(),a);return mapper.lock(id);
    }
    public Map<String,Object> detail(long id) {
        var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");return Map.of("request",accessible(id,a),"events",mapper.events(id));
    }
    public ChangeRequest resubmit(long id,Apply request) {
        var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);approved(p);
        if(!"RETURNED".equals(r.getStatus()))throw BusinessException.badRequest("仅退回延期申请可补充重提");
        unchanged(r,p);validate(p.getEndDate(),request.endDate());
        r.setRequestedSnapshot(snapshot(new Dates(p.getStartDate(),request.endDate())));r.setReason(request.reason());r.setStatus("PENDING");r.setReviewerId(null);r.setReviewComment(null);
        mapper.save(r);mapper.event(r,a.id());mapper.notifySchool(a.schoolId(),p.getId());audit.audit(a.id(),"EXTENSION_RESUBMIT","change_request",id,r.getReason());return r;
    }
    public ChangeRequest review(long id,Review request) {
        var a=portal.actor("SCHOOL_ADMIN");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);portal.requireCurrent(p);
        if(!"PENDING".equals(r.getStatus()))throw BusinessException.badRequest("仅待审延期申请可处理");
        if("APPROVED".equals(request.decision())){
            approved(p);unchanged(r,p);var requested=dates(r.getRequestedSnapshot());
            if(!requested.startDate().equals(p.getStartDate()))throw BusinessException.badRequest("延期不能修改实习开始日期");
            validate(p.getEndDate(),requested.endDate());mapper.extend(p.getId(),requested.endDate());
        }
        r.setStatus(request.decision());r.setReviewerId(a.id());r.setReviewComment(request.comment());mapper.save(r);mapper.event(r,a.id());
        audit.audit(a.id(),"EXTENSION_REVIEW","change_request",id,r.getStatus()+"："+request.comment());
        var recipients=new HashSet<Long>();recipients.add(students.findById(p.getStudentId()).getUserId());
        if(p.getTeacherId()!=null)recipients.add(p.getTeacherId());if(p.getEnterpriseMentorId()!=null)recipients.add(p.getEnterpriseMentorId());
        for(long user:recipients)accounts.notifyUser(user,"实习延期审批结果",request.comment(),"PLACEMENT",p.getId());return r;
    }
}
