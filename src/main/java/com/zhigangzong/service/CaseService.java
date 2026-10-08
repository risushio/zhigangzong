package com.zhigangzong.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.common.*;
import com.zhigangzong.dto.CaseRequests.*;
import com.zhigangzong.entity.StudentCase;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class CaseService {
    private final PortalService portal; private final PortalMapper accounts; private final CaseMapper mapper; private final StudentProfileMapper students; private final ManagementMapper audit; private final ObjectMapper json;
    private StudentCase accessible(long id,PortalMapper.Actor a){var c=mapper.lock(id);if(c==null || !Objects.equals(c.getSchoolId(),a.schoolId()) || ("STUDENT".equals(a.role())&&!Objects.equals(students.findById(c.getStudentId()).getUserId(),a.id())) || ("TEACHER".equals(a.role())&&!Objects.equals(c.getOwnerId(),a.id())))throw BusinessException.notFound("求助或预警");return c;}
    private void open(StudentCase c){if("CLOSED".equals(c.getStatus()))throw BusinessException.badRequest("已关闭记录仅供历史查询");}
    private void event(StudentCase c,PortalMapper.Actor a,String action,String note){try{mapper.event(c.getId(),a.id(),action,note,json.writeValueAsString(c));}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}audit.audit(a.id(),"CASE_"+action,"student_case",c.getId(),note);}
    private void notifyStudent(StudentCase c,String title,String note){accounts.notifyUser(students.findById(c.getStudentId()).getUserId(),title,note,"CASE",c.getId());}
    public PageResult<StudentCase> list(int page,int size){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER");var q=new PageQuery(page,size);return new PageResult<>(mapper.list(a,q.size(),q.offset()),mapper.count(a),page,size);}
    public Map<String,Object> detail(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER");return Map.of("case",accessible(id,a),"events",mapper.events(id));}
    public List<Map<String,Object>> owners(){var a=portal.actor("SCHOOL_ADMIN");return mapper.owners(a.schoolId());}
    public StudentCase create(Create r){var a=portal.actor("STUDENT");var s=accounts.student(a.id());if(s==null)throw BusinessException.badRequest("请先建立学生档案");if(r.placementId()!=null)portal.accessiblePlacement(r.placementId(),a);var c=new StudentCase();c.setSchoolId(a.schoolId());c.setStudentId(s.getId());c.setPlacementId(r.placementId());c.setKind("HELP");c.setTitle(r.title());c.setDescription(r.description());mapper.create(c);c=mapper.lock(c.getId());event(c,a,"CREATED",r.description());mapper.notifySchool(a.schoolId(),c.getId());return c;}
    public StudentCase assign(long id,Assign r){var a=portal.actor("SCHOOL_ADMIN");var c=accessible(id,a);open(c);if(!List.of("OPEN","IN_PROGRESS").contains(c.getStatus()))throw BusinessException.badRequest("仅待处理或处理中可分派");if(mapper.owners(a.schoolId()).stream().noneMatch(u->((Number)u.get("id")).longValue()==r.ownerId()))throw BusinessException.badRequest("请选择本校已开通的管理员或教师");if(Objects.equals(c.getOwnerId(),r.ownerId()))throw BusinessException.badRequest("责任人未变化");c.setOwnerId(r.ownerId());c.setStatus("IN_PROGRESS");mapper.save(c);event(c,a,"ASSIGNED",r.note());accounts.notifyUser(r.ownerId(),"求助或预警已分派",r.note(),"CASE",id);notifyStudent(c,"责任人已分派",r.note());return c;}
    public StudentCase follow(long id,Note r){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER");var c=accessible(id,a);open(c);if("RESOLVED".equals(c.getStatus()))throw BusinessException.badRequest("已有处理结果，请确认或复核");event(c,a,"FOLLOW_UP",r.note());if("STUDENT".equals(a.role())&&c.getOwnerId()!=null)accounts.notifyUser(c.getOwnerId(),"学生补充求助或预警信息",r.note(),"CASE",id);else notifyStudent(c,"处理跟进",r.note());return c;}
    public StudentCase resolve(long id,Note r){var a=portal.actor("SCHOOL_ADMIN","TEACHER");var c=accessible(id,a);if(!Objects.equals(c.getOwnerId(),a.id()) || !"IN_PROGRESS".equals(c.getStatus()))throw BusinessException.badRequest("仅当前责任人可提交处理结果");c.setResolution(r.note());c.setStatus("RESOLVED");c.setStudentConfirmed(false);mapper.save(c);event(c,a,"RESULT",r.note());notifyStudent(c,"请确认处理结果",r.note());return c;}
    public StudentCase confirm(long id,Confirm r){var a=portal.actor("STUDENT");var c=accessible(id,a);if(!"RESOLVED".equals(c.getStatus()) || Boolean.TRUE.equals(c.getStudentConfirmed()))throw BusinessException.badRequest("仅未确认的处理结果可确认或退回");c.setStudentConfirmed("ACCEPT".equals(r.decision()));if("REOPEN".equals(r.decision())){c.setStatus("IN_PROGRESS");c.setResolution(null);}mapper.save(c);event(c,a,r.decision(),r.note());mapper.notifySchool(a.schoolId(),id);if(c.getOwnerId()!=null)accounts.notifyUser(c.getOwnerId(),"学生反馈处理结果",r.note(),"CASE",id);return c;}
    public StudentCase review(long id,Review r){var a=portal.actor("SCHOOL_ADMIN");var c=accessible(id,a);if(!"RESOLVED".equals(c.getStatus()) || !Boolean.TRUE.equals(c.getStudentConfirmed()))throw BusinessException.badRequest("学生确认结果后才能学校复核");c.setStatus("CLOSE".equals(r.decision())?"CLOSED":"IN_PROGRESS");if("RETURN".equals(r.decision())){c.setStudentConfirmed(false);c.setResolution(null);}mapper.save(c);event(c,a,r.decision(),r.note());notifyStudent(c,"学校复核结果",r.note());if(c.getOwnerId()!=null)accounts.notifyUser(c.getOwnerId(),"学校复核结果",r.note(),"CASE",id);return c;}
}
