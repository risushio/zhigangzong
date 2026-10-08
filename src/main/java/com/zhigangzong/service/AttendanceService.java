package com.zhigangzong.service;

import com.zhigangzong.dto.AttendanceRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class AttendanceService {
    private final PortalService portal;
    private final AttendanceWorkflowMapper mapper;
    private final PortalMapper accounts;
    private final StudentProfileMapper students;
    private final ManagementMapper audit;
    private final WorkflowPolicyResolver policies;
    private boolean enabled(InternshipPlacement p){var override=mapper.enabled(p.getId());if(override!=null)return override;var e=policies.resolve(p);return e!=null && e.policy().attendanceEnabled();}

    public Map<String,Object> list(long id) {
        var a=portal.actor("STUDENT","TEACHER","ENTERPRISE_MENTOR","SCHOOL_ADMIN");
        var p=portal.accessiblePlacement(id,a);
        return Map.of("placement",p,"enabled",enabled(p),"records",mapper.records(id),"policyEvents",mapper.policyEvents(id));
    }
    public Map<String,Object> policy(long id,Policy r) {
        var a=portal.actor("SCHOOL_ADMIN");var p=portal.accessiblePlacement(id,a);portal.requireCurrent(p);
        if(enabled(p)==r.enabled())throw BusinessException.badRequest("考勤要求未变化");
        mapper.policy(id,r.enabled());mapper.policyEvent(id,a.id(),r.enabled(),r.note());
        audit.audit(a.id(),"ATTENDANCE_POLICY","internship_placement",id,r.note());return list(id);
    }
    private void active(InternshipPlacement p) {
        portal.requireCurrent(p);
        if(!enabled(p))throw BusinessException.badRequest("本实习未启用考勤要求");
        if(!"APPROVED".equals(p.getSchoolApprovalStatus()) || !"ARRIVED".equals(p.getArrivalStatus()) || p.getTeacherId()==null)
            throw BusinessException.badRequest("需学校批准、确认到岗并分配教师后办理考勤");
    }
    private void dates(InternshipPlacement p,LocalDate date,String type) {
        var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if(date.isBefore(p.getStartDate()) || date.isAfter(p.getEndDate()))throw BusinessException.badRequest("考勤日期必须在实习期间内");
        if("CHECK_IN".equals(type) && !date.equals(today))throw BusinessException.badRequest("签到只能办理今天");
        if("MAKE_UP".equals(type) && !date.isBefore(today))throw BusinessException.badRequest("补签只能办理过去日期");
    }
    public AttendanceRecord apply(long id,Apply request) {
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);active(p);dates(p,request.attendanceDate(),request.recordType());
        if(!mapper.conflicts(id,request.attendanceDate(),0).isEmpty())throw BusinessException.badRequest("当天已有签到或有效申请，请查看原记录");
        var r=new AttendanceRecord();r.setPlacementId(id);r.setAttendanceDate(request.attendanceDate());r.setRecordType(request.recordType());r.setNote(request.note());
        r.setStatus("CHECK_IN".equals(r.getRecordType())?"APPROVED":"PENDING");mapper.create(r);mapper.event(r,a.id(),null);
        audit.audit(a.id(),"ATTENDANCE_APPLY","attendance_record",r.getId(),r.getRecordType()+"："+r.getNote());
        if("PENDING".equals(r.getStatus()))accounts.notifyUser(p.getTeacherId(),"考勤申请待审批",r.getAttendanceDate()+"："+r.getNote(),"PLACEMENT",id);
        return mapper.lookup(r.getId());
    }
    private AttendanceRecord accessible(long id,PortalMapper.Actor a) {
        var snapshot=mapper.lookup(id);if(snapshot==null)throw BusinessException.notFound("考勤记录");
        // Placement lock serializes policy changes, creation, review and resubmission for each internship.
        portal.accessiblePlacement(snapshot.getPlacementId(),a);return mapper.lock(id);
    }
    public Map<String,Object> detail(long id) {
        var a=portal.actor("STUDENT","TEACHER","ENTERPRISE_MENTOR","SCHOOL_ADMIN");
        return Map.of("record",accessible(id,a),"events",mapper.events(id));
    }
    public AttendanceRecord resubmit(long id,Resubmit request) {
        var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);active(p);
        if(!"RETURNED".equals(r.getStatus()))throw BusinessException.badRequest("仅退回申请可以补充重提");
        dates(p,r.getAttendanceDate(),r.getRecordType());
        if(!mapper.conflicts(p.getId(),r.getAttendanceDate(),id).isEmpty())throw BusinessException.badRequest("当天已有其他有效考勤记录");
        r.setStatus("PENDING");r.setNote(request.note());r.setReviewerId(null);mapper.save(r);mapper.event(r,a.id(),null);
        accounts.notifyUser(p.getTeacherId(),"考勤申请重新提交",r.getNote(),"PLACEMENT",p.getId());
        audit.audit(a.id(),"ATTENDANCE_RESUBMIT","attendance_record",id,r.getNote());return r;
    }
    public AttendanceRecord review(long id,Review request) {
        var a=portal.actor("TEACHER");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);portal.requireCurrent(p);
        if(!"PENDING".equals(r.getStatus()) || "CHECK_IN".equals(r.getRecordType()))throw BusinessException.badRequest("仅待审批的请假或补签申请可处理");
        r.setStatus(request.decision());r.setReviewerId(a.id());mapper.save(r);mapper.event(r,a.id(),request.feedback());
        accounts.notifyUser(students.findById(p.getStudentId()).getUserId(),"考勤审批结果",request.feedback(),"PLACEMENT",p.getId());
        audit.audit(a.id(),"ATTENDANCE_REVIEW","attendance_record",id,request.decision()+"："+request.feedback());return r;
    }
}
