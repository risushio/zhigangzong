package com.zhigangzong.service;

import com.zhigangzong.common.*;
import com.zhigangzong.dto.PortalRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PortalService {
    private final PortalMapper mapper;
    private final ManagementMapper management;
    private final StudentProfileMapper students;
    private final UserAccountMapper users;
    private final JobPositionMapper jobs;
    private final PasswordEncoder passwords;
    private final SelfPlacementMapper selfPlacements;
    private final FileMapper files;

    PortalMapper.Actor actor(String... roles) {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        var a=authentication==null?null:mapper.actor(authentication.getName());
        if(a==null || !List.of(roles).contains(a.role())) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","无权执行此操作");
        if(List.of("RECRUITER","ENTERPRISE_MENTOR").contains(a.role()) && a.enterpriseId()==null) throw BusinessException.badRequest("招聘账号尚未绑定企业");
        return a;
    }
    private StudentProfile student(PortalMapper.Actor a) {
        var s=mapper.student(a.id());
        if(s==null) throw BusinessException.badRequest("请联系管理员建立学生档案");
        return s;
    }
    private void scope(PortalMapper.Actor a,StudentProfile s,Long enterpriseId) {
        var u=s==null?null:users.findById(s.getUserId());
        if(u==null || u.getSchoolId()!=a.schoolId() || (a.role().equals("STUDENT") && s.getUserId()!=a.id()) || (a.role().equals("RECRUITER") && !Objects.equals(a.enterpriseId(),enterpriseId)))
            throw BusinessException.notFound("业务记录");
    }
    private void audit(PortalMapper.Actor a,String action,String resource,long id,String note) {
        management.audit(a.id(),action,resource,id,note);
    }
    private void dates(LocalDate from,LocalDate to) {
        if(from!=null && to!=null && to.isBefore(from)) throw BusinessException.badRequest("结束日期不能早于开始日期");
    }
    public Map<String,Object> account(Account r) {
        var a=actor("SCHOOL_ADMIN");
        var u=mapper.lockUser(r.userId());
        if(u==null || u.getSchoolId()!=a.schoolId()) throw BusinessException.notFound("本校用户");
        if(!List.of("STUDENT","RECRUITER","TEACHER","ENTERPRISE_MENTOR").contains(u.getRole())) throw BusinessException.badRequest("当前支持开通学生、教师、招聘及企业导师账号");
        if(r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw BusinessException.badRequest("密码最多 72 UTF-8 字节");
        if(u.getRole().equals("STUDENT")) {
            if(mapper.student(u.getId())==null) throw BusinessException.badRequest("请先建立学生档案");
            if(r.enterpriseId()!=null) throw BusinessException.badRequest("学生账号不能绑定企业");
        } else if(!u.getRole().equals("TEACHER")) {
            if(r.enterpriseId()==null || management.lockEnterprise(r.enterpriseId())==null) throw BusinessException.badRequest("请选择有效企业");
            mapper.member(u.getId(),r.enterpriseId());
        } else if(r.enterpriseId()!=null) throw BusinessException.badRequest("教师账号不能绑定企业");
        mapper.account(u.getId(),r.username(),passwords.encode(r.password()));
        audit(a,"ACCOUNT_CREATE","user_account",u.getId(),"开通业务账号");
        return Map.of("username",r.username(),"userId",u.getId(),"role",u.getRole());
    }
    public StudentProfile profile() {return student(actor("STUDENT"));}
    public StudentProfile profile(Profile r) {
        var a=actor("STUDENT");var s=student(a); dates(r.availableFrom(),r.availableTo());
        mapper.profile(a.id(),r);audit(a,"UPDATE","student_profile",s.getId(),"更新本人档案");return mapper.student(a.id());
    }
    public PageResult<Map<String,Object>> jobs(String q,int page,int size) {
        actor("STUDENT","RECRUITER","SCHOOL_ADMIN");var p=new PageQuery(page,size);
        if(q.length()>100)throw BusinessException.badRequest("关键词最多 100 字");
        return new PageResult<>(mapper.jobs(q,p.size(),p.offset()),mapper.jobCount(q),page,size);
    }
    public PageResult<Map<String,Object>> applications(int page,int size) {
        var a=actor("STUDENT","RECRUITER","SCHOOL_ADMIN");var p=new PageQuery(page,size);
        return new PageResult<>(mapper.applications(a,size,p.offset()),mapper.applicationCount(a),page,size);
    }
    private JobApplication accessibleApplication(long id,PortalMapper.Actor a) {
        var application=mapper.application(id);
        if(application==null)throw BusinessException.notFound("申请");
        var job=jobs.findById(application.getJobId());
        scope(a,students.findById(application.getStudentId()),job.getEnterpriseId());
        return application;
    }
    public Map<String,Object> application(long id) {
        var a=actor("STUDENT","RECRUITER","SCHOOL_ADMIN");
        var application=accessibleApplication(id,a);
        var result=new LinkedHashMap<String,Object>();result.put("application",application);result.put("events",mapper.events(id));var resume=files.resume(id);if(resume!=null && !(a.role().equals("RECRUITER") && "WITHDRAWN".equals(application.getRecruitmentStatus())))result.put("resumeFile",resume);return result;
    }
    public JobApplication apply(Apply r) {
        var a=actor("STUDENT");var s=student(a);mapper.lockStudent(s.getId());
        if(r.resumeFileId()!=null){var f=files.file(r.resumeFileId());if(f==null || !Objects.equals(f.getOwnerUserId(),a.id()) || !Objects.equals(f.getSchoolId(),a.schoolId()) || !"RESUME".equals(f.getKind()))throw BusinessException.notFound("本人简历文件");}
        else if(r.resumeRef()==null || r.resumeRef().isBlank())throw BusinessException.badRequest("请选择已上传简历或填写本次分享的简历引用");
        var snapshot=jobs.findById(r.jobId());if(snapshot==null)throw BusinessException.notFound("岗位");
        var e=management.lockEnterprise(snapshot.getEnterpriseId());var job=management.lockJob(r.jobId());
        if(!Objects.equals(e.getId(),job.getEnterpriseId()))throw BusinessException.badRequest("岗位所属企业已变化，请重试");
        if(!"APPROVED".equals(e.getReviewStatus()) || !"APPROVED".equals(job.getReviewStatus()) || !"PUBLISHED".equals(job.getPublishStatus()) || (job.getApplicationDeadline()!=null && job.getApplicationDeadline().isBefore(LocalDate.now())))
            throw BusinessException.badRequest("岗位尚未开放或已截止投递");
        var application=new JobApplication();application.setStudentId(s.getId());application.setJobId(job.getId());application.setResumeRef(r.resumeFileId()==null?r.resumeRef():"已上传简历文件 #"+r.resumeFileId());
        mapper.apply(application);if(r.resumeFileId()!=null)files.share(application.getId(),r.resumeFileId());mapper.event(application.getId(),a.id(),null,"APPLIED","学生提交申请并向招聘方分享本次简历引用");
        mapper.notifyRecruiters(a.schoolId(),job.getEnterpriseId(),application.getId(),"收到新的岗位申请",job.getTitle()+"：请在候选人管理中查看。");
        audit(a,"APPLY","job_application",application.getId(),"学生投递岗位");return mapper.application(application.getId());
    }
    public JobApplication transition(long id,Transition r) {
        var a=actor("STUDENT","RECRUITER");var snapshot=accessibleApplication(id,a);
        mapper.lockStudent(snapshot.getStudentId());var application=mapper.lockApplication(id);
        String from=application.getRecruitmentStatus(),to=r.status();
        boolean allowed=a.role().equals("STUDENT")
            ? (to.equals("ACCEPTED")&&from.equals("OFFERED")) || (to.equals("WITHDRAWN")&&List.of("APPLIED","INTERVIEW","OFFERED").contains(from))
            : (to.equals("INTERVIEW")&&from.equals("APPLIED")) || (to.equals("OFFERED")&&List.of("APPLIED","INTERVIEW").contains(from)) || (to.equals("REJECTED")&&List.of("APPLIED","INTERVIEW").contains(from));
        if(!allowed)throw BusinessException.badRequest("当前状态或角色不允许此操作");
        if(to.equals("INTERVIEW")) {
            if(r.interviewAt()==null || !r.interviewAt().isAfter(LocalDateTime.now()))throw BusinessException.badRequest("请填写未来的面试时间");
            application.setInterviewAt(r.interviewAt());
        }
        if(to.equals("OFFERED")) {
            if(r.offerDetails()==null || r.offerDetails().isBlank())throw BusinessException.badRequest("请填写录用说明");
            application.setOfferDetails(r.offerDetails());
        }
        application.setRecruitmentStatus(to);application.setStudentConfirmed(to.equals("ACCEPTED"));mapper.transition(application);
        mapper.event(id,a.id(),from,to,r.note());audit(a,"TRANSITION","job_application",id,to+": "+r.note());
        if(a.role().equals("STUDENT"))mapper.notifyRecruiters(a.schoolId(),jobs.findById(application.getJobId()).getEnterpriseId(),id,"学生更新申请状态",to+"："+r.note());
        mapper.notifyUser(students.findById(application.getStudentId()).getUserId(),"申请状态更新",from+" → "+to+"："+r.note(),"APPLICATION",id);
        return application;
    }
    public List<InternshipBatch> batches() {var a=actor("STUDENT");return mapper.batches(a.departmentId());}
    private void placementDates(Placement r,InternshipBatch b,JobPosition j) {
        dates(r.startDate(),r.endDate());
        if(r.startDate().isBefore(b.getStartDate()) || r.endDate().isAfter(b.getEndDate()) || (j.getStartDate()!=null&&r.startDate().isBefore(j.getStartDate())) || (j.getEndDate()!=null&&r.endDate().isAfter(j.getEndDate())))
            throw BusinessException.badRequest("申请时间必须在批次及岗位实习时间内");
    }
    public InternshipPlacement submitPlacement(Placement r) {
        var a=actor("STUDENT");var s=student(a);mapper.lockStudent(s.getId());
        var application=accessibleApplication(r.applicationId(),a);
        if(!application.getRecruitmentStatus().equals("ACCEPTED"))throw BusinessException.badRequest("请先确认录用，再提交学校审批");
        var b=mapper.batch(r.batchId());if(b==null || !Objects.equals(b.getDepartmentId(),a.departmentId()))throw BusinessException.badRequest("请选择本学院批次");
        var j=jobs.findById(application.getJobId());placementDates(r,b,j);
        if(mapper.duplicatePlacement(application.getId(),s.getId(),b.getId())>0)throw BusinessException.badRequest("该申请或该批次已有实习记录，请查看已有审批");
        var p=new InternshipPlacement();p.setStudentId(s.getId());p.setBatchId(b.getId());p.setApplicationId(application.getId());p.setEnterpriseId(j.getEnterpriseId());p.setJobId(j.getId());p.setPositionTitle(j.getTitle());p.setStartDate(r.startDate());p.setEndDate(r.endDate());
        mapper.placement(p);mapper.notifySchool(a.schoolId(),p.getId());audit(a,"SUBMIT","internship_placement",p.getId(),"录用确认后独立提交学校审批");return mapper.lockPlacement(p.getId());
    }
    public PageResult<Map<String,Object>> placements(int page,int size) {
        var a=actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=new PageQuery(page,size);
        return new PageResult<>(mapper.placements(a,size,p.offset()),mapper.placementCount(a),page,size);
    }
    InternshipPlacement accessiblePlacement(long id,PortalMapper.Actor a) {
        var p=mapper.lockPlacement(id);if(p==null)throw BusinessException.notFound("实习记录");scope(a,students.findById(p.getStudentId()),p.getEnterpriseId());
        if((a.role().equals("TEACHER") && !Objects.equals(p.getTeacherId(),a.id())) || (a.role().equals("ENTERPRISE_MENTOR") && (!Objects.equals(p.getEnterpriseMentorId(),a.id()) || !Objects.equals(p.getEnterpriseId(),a.enterpriseId())))) throw BusinessException.notFound("实习记录");return p;
    }
    void requireNotReplaced(InternshipPlacement p) {
        if("APPROVED".equals(p.getArchiveStatus()))throw BusinessException.badRequest("实习已结项归档，仅可查看历史或导出档案");
        if(p.getReplacementPlacementId()!=null)throw BusinessException.badRequest("原实习已被新安排替代，仅可查看历史，请在新实习记录办理");
    }
    void requireCurrent(InternshipPlacement p) {
        requireNotReplaced(p);
        if(p.getTerminationRequestId()!=null)throw BusinessException.badRequest("实习已终止，仅可查看历史；继续实习请申请关联新安排");
    }
    public Map<String,Object> placement(long id) {
        var a=actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=accessiblePlacement(id,a);var result=new LinkedHashMap<String,Object>();result.put("placement",p);result.put("approvals",mapper.approvals(id));var detail=selfPlacements.detail(id);if(detail!=null){result.put("selfDeclaration",detail);result.put("selfHistory",selfPlacements.events(id));}return result;
    }
    public InternshipPlacement approve(long id,Approval r) {
        var a=actor("SCHOOL_ADMIN");var p=accessiblePlacement(id,a);requireCurrent(p);
        if(!p.getSchoolApprovalStatus().equals("PENDING"))throw BusinessException.badRequest("仅待审批申请可以处理");
        if("SELF".equals(p.getSource()) && "APPROVED".equals(r.decision())){var e=management.lockEnterprise(p.getEnterpriseId());if(e==null || List.of("SUSPENDED","REJECTED").contains(e.getReviewStatus()))throw BusinessException.badRequest("单位已暂停或未通过审核，不能批准实习");}
        mapper.approval(id,r.decision());mapper.approvalRecord(id,a.id(),r.decision(),r.comment());
        audit(a,"APPROVAL","internship_placement",id,r.decision()+": "+r.comment());
        mapper.notifyUser(students.findById(p.getStudentId()).getUserId(),"学校审批结果",r.decision()+"："+r.comment(),"PLACEMENT",id);
        return mapper.lockPlacement(id);
    }
    public InternshipPlacement resubmit(long id,Placement r) {
        var a=actor("STUDENT");var p=accessiblePlacement(id,a);requireCurrent(p);
        if(!"PLATFORM".equals(p.getSource()))throw BusinessException.badRequest("自主申报请使用自主申报补充入口");
        if(!p.getSchoolApprovalStatus().equals("RETURNED"))throw BusinessException.badRequest("只有退回的申请可以补充提交");
        if(!Objects.equals(p.getApplicationId(),r.applicationId())||!Objects.equals(p.getBatchId(),r.batchId()))throw BusinessException.badRequest("补充提交不能更换原申请或批次");
        placementDates(r,mapper.batch(p.getBatchId()),jobs.findById(p.getJobId()));
        p.setStartDate(r.startDate());p.setEndDate(r.endDate());mapper.resubmit(p);
        mapper.notifySchool(a.schoolId(),id);
        audit(a,"RESUBMIT","internship_placement",id,"学生补充后重新提交，保留既往审批记录");return mapper.lockPlacement(id);
    }
    public PageResult<Map<String,Object>> notifications(int page,int size) {
        var a=actor("STUDENT","RECRUITER","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=new PageQuery(page,size);
        return new PageResult<>(mapper.notifications(a.id(),size,p.offset()),mapper.notificationCount(a.id()),page,size);
    }
    public void read(long id) {var a=actor("STUDENT","RECRUITER","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");if(mapper.read(id,a.id())==0)throw BusinessException.notFound("通知");}
}
