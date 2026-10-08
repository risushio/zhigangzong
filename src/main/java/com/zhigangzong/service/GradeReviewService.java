package com.zhigangzong.service;
import com.zhigangzong.dto.GradeReviewRequests.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class GradeReviewService {
 private final PortalService portal; private final PortalMapper accounts; private final GradeService grades; private final GradeMapper evaluations; private final GradeReviewMapper mapper; private final StudentProfileMapper students; private final ManagementMapper audit;
 private void active(InternshipPlacement p){portal.requireCurrent(p);if(!"APPROVED".equals(p.getSchoolApprovalStatus())||!"ARRIVED".equals(p.getArrivalStatus()))throw BusinessException.badRequest("学校批准并到岗后才能复核成绩");}
 private String snapshot(InternshipPlacement p){return grades.write(grades.calculate(p));}
 private GradeReview accessible(long id,PortalMapper.Actor a){var r=mapper.lookup(id);if(r==null)throw BusinessException.notFound("成绩复核申请");portal.accessiblePlacement(r.getPlacementId(),a);return mapper.lock(id);}
 Long approved(InternshipPlacement p){if("APPROVED".equals(p.getArchiveStatus()))return grades.archivedReviewId(p);String current=snapshot(p);if(!mapper.active(p.getId()).isEmpty())return null;return mapper.list(p.getId()).stream().filter(r->"APPROVED".equals(r.getStatus())&&current.equals(r.getRequestedSnapshot())).map(GradeReview::getId).findFirst().orElse(null);}
 public Map<String,Object> list(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var p=portal.accessiblePlacement(id,a);var result=new LinkedHashMap<String,Object>();result.put("placement",p);result.put("requests",mapper.list(id));try{var approved=approved(p);if(approved!=null)result.put("approvedRequestId",approved);}catch(BusinessException ex){result.put("notReady",ex.getMessage());}return result;}
 public Map<String,Object> detail(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");return Map.of("request",accessible(id,a),"events",mapper.events(id));}
 public GradeReview apply(long id,Apply request){var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);active(p);if(!mapper.active(id).isEmpty())throw BusinessException.badRequest("已有待审或退回成绩复核，请先处理原申请");var r=new GradeReview();r.setPlacementId(id);r.setReason(request.reason());r.setStatus("PENDING");r.setOriginalSnapshot(snapshot(p));r.setRequestedSnapshot(r.getOriginalSnapshot());mapper.create(r);mapper.event(r,a.id());mapper.notifySchool(a.schoolId(),id);audit.audit(a.id(),"GRADE_REVIEW_APPLY","grade_review_request",r.getId(),request.reason());return mapper.lock(r.getId());}
 public GradeReview resubmit(long id,Apply request){var a=portal.actor("STUDENT");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);active(p);if(!"RETURNED".equals(r.getStatus()))throw BusinessException.badRequest("仅退回复核可补充重提");r.setRequestedSnapshot(snapshot(p));r.setReason(request.reason());r.setStatus("PENDING");r.setReviewerId(null);r.setReviewComment(null);mapper.save(r);mapper.event(r,a.id());mapper.notifySchool(a.schoolId(),p.getId());return r;}
 public GradeReview review(long id,Review request){var a=portal.actor("SCHOOL_ADMIN");var r=accessible(id,a);var p=portal.accessiblePlacement(r.getPlacementId(),a);active(p);if(!"PENDING".equals(r.getStatus())&&!("RETURNED".equals(r.getStatus())&&"REJECTED".equals(request.decision())))throw BusinessException.badRequest("仅待审可复核，退回申请仅可拒绝关闭");
  if("APPROVED".equals(request.decision())&&!snapshot(p).equals(r.getRequestedSnapshot()))throw BusinessException.badRequest("评分、导师或规则已变化，请退回核对后重提");
  if(new HashSet<>(request.returnTypes()).size()!=request.returnTypes().size()||(!"RETURNED".equals(request.decision())&&!request.returnTypes().isEmpty()))throw BusinessException.badRequest("请仅在退回时选择不重复的评价方");
  if("RETURNED".equals(request.decision())){if(request.returnTypes().isEmpty())throw BusinessException.badRequest("请选择需要补充的评价方");var entries=evaluations.entries(p.getId());for(String t:request.returnTypes()){var v=entries.stream().filter(e->t.equals(e.getType())).findFirst().orElseThrow(()->BusinessException.badRequest("评价记录不存在"));v.setStatus("RETURNED");evaluations.save(v);v=evaluations.entries(p.getId()).stream().filter(e->t.equals(e.getType())).findFirst().orElseThrow();evaluations.event(v.getId(),a.id(),"RETURNED",request.comment(),grades.write(v));accounts.notifyUser(v.getEvaluatorId(),"评价退回补充",request.comment(),"PLACEMENT",p.getId());}}
  r.setStatus(request.decision());r.setReviewerId(a.id());r.setReviewComment(request.comment());mapper.save(r);mapper.event(r,a.id());audit.audit(a.id(),"GRADE_REVIEW","grade_review_request",id,request.decision()+"："+request.comment());accounts.notifyUser(students.findById(p.getStudentId()).getUserId(),"成绩复核结果",request.comment(),"PLACEMENT",p.getId());return r;
 }
}
