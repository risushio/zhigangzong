package com.zhigangzong.service;
import com.zhigangzong.common.*;
import com.zhigangzong.dto.MatchRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class MatchActionService {
 private final PortalService portal;private final PortalMapper accounts;private final MatchActionMapper mapper;private final ManagementMapper audit;private final StudentProfileMapper students;private final UserAccountMapper users;
 private StudentProfile student(PortalMapper.Actor a){var s=accounts.student(a.id());if(s==null)throw BusinessException.badRequest("请先建立学生档案");accounts.lockStudent(s.getId());return s;}
 private void open(long id){var j=audit.lockJob(id);var e=j==null?null:audit.lockEnterprise(j.getEnterpriseId());if(j==null||e==null||!"APPROVED".equals(e.getReviewStatus())||!"APPROVED".equals(j.getReviewStatus())||!"PUBLISHED".equals(j.getPublishStatus())||(j.getApplicationDeadline()!=null&&j.getApplicationDeadline().isBefore(LocalDate.now())))throw BusinessException.badRequest("岗位尚未开放或已截止");}
 public PageResult<Map<String,Object>> favorites(int page,int size){var s=student(portal.actor("STUDENT"));var p=new PageQuery(page,size);return new PageResult<>(mapper.favorites(s.getId(),p.size(),p.offset()),mapper.count(s.getId()),page,size);}
 public void favorite(long id,boolean add){var a=portal.actor("STUDENT");var s=student(a);if(add){open(id);mapper.favorite(s.getId(),id);}else mapper.unfavorite(s.getId(),id);audit.audit(a.id(),add?"FAVORITE":"UNFAVORITE","job_position",id,"本人岗位收藏");}
 public MatchFeedback feedback(Feedback r){var a=portal.actor("STUDENT");var s=student(a);open(r.jobId());var f=new MatchFeedback();f.setStudentId(s.getId());f.setJobId(r.jobId());f.setFeedbackType(r.feedbackType());f.setReason(r.reason());mapper.feedback(f);mapper.notifySchool(a.schoolId(),f.getId());audit.audit(a.id(),"MATCH_FEEDBACK","match_feedback",f.getId(),r.feedbackType());return f;}
 public PageResult<Map<String,Object>> feedbacks(int page,int size){var a=portal.actor("STUDENT","SCHOOL_ADMIN");var p=new PageQuery(page,size);return new PageResult<>(mapper.feedbacks(a,p.size(),p.offset()),mapper.feedbackCount(a),page,size);}
 public void handle(long id,Handle r){var a=portal.actor("SCHOOL_ADMIN");var f=mapper.lock(id);var s=f==null?null:students.findById(f.getStudentId());var u=s==null?null:users.findById(s.getUserId());if(u==null||u.getSchoolId()!=a.schoolId())throw BusinessException.notFound("推荐反馈");if(mapper.resolved(id)>0)throw BusinessException.badRequest("反馈已办理");mapper.resolve(id,a.id(),r.note());accounts.notifyUser(u.getId(),"推荐反馈已办理",r.note(),"MATCH_FEEDBACK",id);audit.audit(a.id(),"MATCH_HANDLE","match_feedback",id,r.note());}
}
