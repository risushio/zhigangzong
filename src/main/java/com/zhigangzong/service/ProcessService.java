package com.zhigangzong.service;

import com.zhigangzong.dto.PortalRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class ProcessService {
    private final PortalService portal;
    private final PortalMapper accounts;
    private final ProcessMapper mapper;
    private final ManagementMapper audit;
    private final StudentProfileMapper students;
    private final WorkflowPolicyResolver policies;

    public List<Map<String,Object>> mentors(long id) {
        var a=portal.actor("SCHOOL_ADMIN");var p=portal.accessiblePlacement(id,a);
        return mapper.mentors(a.schoolId(),p.getEnterpriseId());
    }
    public InternshipPlacement assign(long id,Mentors r) {
        var a=portal.actor("SCHOOL_ADMIN");var p=portal.accessiblePlacement(id,a);portal.requireCurrent(p);
        if(!"APPROVED".equals(p.getSchoolApprovalStatus()))throw BusinessException.badRequest("学校批准后才能分配双导师");
        var eligible=mapper.mentors(a.schoolId(),p.getEnterpriseId());
        boolean teacher=eligible.stream().anyMatch(u->Objects.equals(((Number)u.get("id")).longValue(),r.teacherId()) && "TEACHER".equals(u.get("role")));
        boolean mentor=eligible.stream().anyMatch(u->Objects.equals(((Number)u.get("id")).longValue(),r.enterpriseMentorId()) && "ENTERPRISE_MENTOR".equals(u.get("role")));
        if(!teacher || !mentor)throw BusinessException.badRequest("请选择本校已开通教师和本实习企业已开通导师");
        if(Objects.equals(p.getTeacherId(),r.teacherId()) && Objects.equals(p.getEnterpriseMentorId(),r.enterpriseMentorId()))throw BusinessException.badRequest("导师分配未变化");
        mapper.assign(id,r.teacherId(),r.enterpriseMentorId());
        mapper.event(id,a.id(),"MENTORS_ASSIGNED",r.note(),r.teacherId(),r.enterpriseMentorId(),null);
        audit.audit(a.id(),"ASSIGN_MENTORS","internship_placement",id,r.note());
        for(long user:List.of(r.teacherId(),r.enterpriseMentorId(),students.findById(p.getStudentId()).getUserId()))accounts.notifyUser(user,"双导师已分配",r.note(),"PLACEMENT",id);
        return accounts.lockPlacement(id);
    }
    public InternshipPlacement arrive(long id,Arrival r) {
        var a=portal.actor("ENTERPRISE_MENTOR");var p=portal.accessiblePlacement(id,a);portal.requireCurrent(p);
        if(!"APPROVED".equals(p.getSchoolApprovalStatus()) || p.getTeacherId()==null || p.getEnterpriseMentorId()==null)throw BusinessException.badRequest("需学校批准并完成双导师分配");
        if(!"NOT_ARRIVED".equals(p.getArrivalStatus()))throw BusinessException.badRequest("已确认到岗，不能重复办理");
        if(r.arrivalDate().isBefore(p.getStartDate()) || r.arrivalDate().isAfter(p.getEndDate()) || r.arrivalDate().isAfter(LocalDate.now()))throw BusinessException.badRequest("到岗日期必须在实习期间且不能晚于今天");
        mapper.arrive(id);mapper.event(id,a.id(),"ARRIVED",r.note(),p.getTeacherId(),p.getEnterpriseMentorId(),r.arrivalDate());
        audit.audit(a.id(),"ARRIVAL","internship_placement",id,r.note());
        for(long user:List.of(p.getTeacherId(),students.findById(p.getStudentId()).getUserId()))accounts.notifyUser(user,"企业导师已确认到岗",r.arrivalDate()+"："+r.note(),"PLACEMENT",id);
        return accounts.lockPlacement(id);
    }
    public Map<String,Object> process(long id) {
        var a=portal.actor("STUDENT","TEACHER","ENTERPRISE_MENTOR","SCHOOL_ADMIN");var p=portal.accessiblePlacement(id,a);
        // Unsubmitted drafts remain private to their author and school administrators.
        var reports=mapper.reports(id).stream().filter(r->!"DRAFT".equals(r.getStatus()) || List.of("STUDENT","SCHOOL_ADMIN").contains(a.role())).toList();
        return Map.of("placement",p,"events",mapper.events(id),"reports",reports);
    }
    private void active(InternshipPlacement p) {
        portal.requireCurrent(p);
        if(!"APPROVED".equals(p.getSchoolApprovalStatus()) || !"ARRIVED".equals(p.getArrivalStatus()))throw BusinessException.badRequest("学校批准且确认到岗后才能填写周报");
    }
    private void fill(ProgressReport report,Report r,InternshipPlacement p) {
        if(r.periodEnd().isBefore(r.periodStart()) || r.periodStart().isBefore(p.getStartDate()) || r.periodEnd().isAfter(p.getEndDate()))throw BusinessException.badRequest("周报日期必须有序且在实习期间内");
        policies.report(p,r.periodStart(),r.periodEnd());
        report.setTitle(r.title());report.setPeriodStart(r.periodStart());report.setPeriodEnd(r.periodEnd());report.setContent(r.content());
    }
    public ProgressReport create(long id,Report r) {
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);active(p);
        var report=new ProgressReport();report.setPlacementId(id);fill(report,r,p);report.setStatus("DRAFT");mapper.create(report);
        mapper.reportEvent(report,a.id(),"DRAFT_SAVED");audit.audit(a.id(),"DRAFT_SAVE","progress_report",report.getId(),"学生保存周报草稿");return mapper.report(report.getId());
    }
    private ProgressReport accessible(long id,PortalMapper.Actor a) {
        // Locate without locking, then lock placement before report in every mutation.
        var snapshot=mapper.reportsForLookup(id);if(snapshot==null)throw BusinessException.notFound("周报");
        portal.accessiblePlacement(snapshot.getPlacementId(),a);var r=mapper.report(id);
        if("DRAFT".equals(r.getStatus()) && !List.of("STUDENT","SCHOOL_ADMIN").contains(a.role()))throw BusinessException.notFound("周报");
        return r;
    }
    public Map<String,Object> report(long id) {var a=portal.actor("STUDENT","TEACHER","ENTERPRISE_MENTOR","SCHOOL_ADMIN");return Map.of("report",accessible(id,a),"events",mapper.reportEvents(id));}
    public ProgressReport save(long id,Report r) {
        var a=portal.actor("STUDENT");var report=accessible(id,a);var p=portal.accessiblePlacement(report.getPlacementId(),a);active(p);
        if(!List.of("DRAFT","RETURNED").contains(report.getStatus()))throw BusinessException.badRequest("仅草稿和退回周报可修改");
        fill(report,r,p);mapper.save(report);mapper.reportEvent(report,a.id(),"DRAFT_SAVED");audit.audit(a.id(),"DRAFT_SAVE","progress_report",id,"学生修改周报草稿");return report;
    }
    public ProgressReport submit(long id) {
        var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);active(p);
        if(!List.of("DRAFT","RETURNED").contains(r.getStatus()) || p.getTeacherId()==null)throw BusinessException.badRequest("当前周报不能提交或缺少指导教师");
        policies.report(p,r.getPeriodStart(),r.getPeriodEnd());
        r.setStatus("SUBMITTED");r.setReviewerId(null);r.setFeedback(null);mapper.save(r);mapper.reportEvent(r,a.id(),"SUBMITTED");
        audit.audit(a.id(),"SUBMIT","progress_report",id,"学生提交周报");accounts.notifyUser(p.getTeacherId(),"周报待批阅",r.getTitle(),"REPORT",id);return r;
    }
    public ProgressReport review(long id,ReportReview request) {
        var a=portal.actor("TEACHER");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);active(p);
        if(!"SUBMITTED".equals(r.getStatus()))throw BusinessException.badRequest("仅已提交周报可批阅或退回");
        r.setStatus(request.decision());r.setReviewerId(a.id());r.setFeedback(request.feedback());mapper.save(r);mapper.reportEvent(r,a.id(),request.decision());
        audit.audit(a.id(),"REVIEW","progress_report",id,request.decision()+"："+request.feedback());
        accounts.notifyUser(students.findById(p.getStudentId()).getUserId(),"周报批阅结果",request.feedback(),"REPORT",id);return r;
    }
}
