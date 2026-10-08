package com.zhigangzong.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.JobFilter;
import com.zhigangzong.dto.WarningRequests.Policy;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class BatchStatisticsService {
 private final PortalService portal;private final PortalMapper accounts;private final WarningMapper warnings;private final BatchStatisticsMapper mapper;private final JobPositionMapper jobs;private final WorkflowPolicyResolver rules;private final ObjectMapper json;
 public Map<String,Object> statistics(long id){var a=portal.actor("SCHOOL_ADMIN");var b=accounts.batch(id);if(b==null||warnings.departmentSchool(b.getDepartmentId())!=a.schoolId())throw BusinessException.notFound("本校批次");var cohort=mapper.students(a.schoolId(),b.getDepartmentId());var places=new HashMap<Long,InternshipPlacement>();for(var p:mapper.placements(id))places.putIfAbsent(p.getStudentId(),p);int placed=0,completed=0,matched=0,known=0,materialMissing=0,materialConfigured=0;var details=new ArrayList<Map<String,Object>>();
  for(var s:cohort){var p=places.get(s.getId());var row=new LinkedHashMap<String,Object>();row.put("studentId",s.getId());row.put("studentNo",s.getStudentNo());row.put("major",s.getMajor());row.put("placed",p!=null);row.put("completed",p!=null&&"APPROVED".equals(p.getArchiveStatus()));if(p!=null){placed++;if("APPROVED".equals(p.getArchiveStatus()))completed++;row.put("placementId",p.getId());var job=p.getJobId()==null?null:jobs.findById(p.getJobId());var majors=job==null?List.<String>of():JobFilter.tokens(job.getRequiredMajor());boolean hasMajor=!majors.isEmpty()&&!majors.contains("不限");boolean match=hasMajor&&majors.contains(s.getMajor().trim().toLowerCase(Locale.ROOT));if(hasMajor){known++;if(match)matched++;}row.put("professionalMatch",hasMajor?(match?"MATCHED":"CONFLICT"):"UNKNOWN");List<String> kinds=null;var rule=rules.resolve(p);if(rule!=null)kinds=rule.policy().requiredKinds();else{String setting=warnings.policy(id);if(setting!=null)try{var r=json.readValue(setting,Policy.class);if(r.materials())kinds=r.requiredKinds();}catch(java.io.IOException ex){throw BusinessException.badRequest("统计材料规则无效");}}
    if(kinds!=null){materialConfigured++;var missing=new ArrayList<String>();for(var k:kinds)if(warnings.material(p.getId(),k)==0)missing.add(k);if(!missing.isEmpty())materialMissing++;row.put("missingKinds",missing);}else row.put("materialRule","UNCONFIGURED");}
   details.add(row);
  }
  var result=new LinkedHashMap<String,Object>();result.put("batch",b);result.put("cohort",cohort.size());result.put("placed",placed);result.put("unplaced",cohort.size()-placed);result.put("completed",completed);result.put("placementRate",rate(placed,cohort.size()));result.put("completionRate",rate(completed,cohort.size()));result.put("matched",matched);result.put("professionalKnown",known);result.put("professionalUnknown",placed-known);result.put("professionalMatchRate",rate(matched,known));result.put("materialConfigured",materialConfigured);result.put("materialMissing",materialMissing);result.put("materialUnconfigured",placed-materialConfigured);result.put("details",details);return result;
 }
 private BigDecimal rate(int count,int total){return total==0?null:BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);}
}
