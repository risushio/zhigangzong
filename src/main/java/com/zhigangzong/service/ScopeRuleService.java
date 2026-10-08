package com.zhigangzong.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.ScopeRuleRequests.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class ScopeRuleService {
 private final PortalService portal;private final ScopeRuleMapper mapper;private final WorkflowPolicyResolver resolver;private final ObjectMapper json;private final ManagementMapper audit;
 public Map<String,Object> list(){var a=portal.actor("SCHOOL_ADMIN");return Map.of("policies",mapper.list(a.schoolId()),"departments",mapper.departments(a.schoolId()),"events",mapper.history(a.schoolId()));}
 public Map<String,Object> save(Save r)throws java.io.IOException {var a=portal.actor("SCHOOL_ADMIN");if(!Objects.equals(mapper.school(r.departmentId()),a.schoolId()))throw BusinessException.notFound("本校学院");if(r.batchId()!=null&&!Objects.equals(mapper.batchDepartment(r.batchId()),r.departmentId()))throw BusinessException.badRequest("批次必须属于所选学院");var p=r.policy();if(new HashSet<>(p.requiredKinds()).size()!=p.requiredKinds().size())throw BusinessException.badRequest("材料类型不能重复");var g=p.grading();if(g.studentWeight().add(g.enterpriseWeight()).add(g.teacherWeight()).compareTo(new java.math.BigDecimal("100"))!=0)throw BusinessException.badRequest("评分权重须合计 100%");String major=r.major()==null?"":r.major().trim();mapper.save(r.departmentId(),r.batchId(),r.batchId()==null?0:r.batchId(),major,json.writeValueAsString(p));var row=mapper.find(r.departmentId(),r.batchId()==null?0:r.batchId(),major);long id=((Number)row.get("id")).longValue();mapper.event(id,a.id());audit.audit(a.id(),"WORKFLOW_POLICY","scoped_workflow_policy",id,g.note());return list();}
 public Map<String,Object> effective(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var e=resolver.resolve(portal.accessiblePlacement(id,a));return e==null?Map.of("configured",false):Map.of("configured",true,"rule",e);}
}
