package com.zhigangzong.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.ScopeRuleRequests.Policy;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Component @RequiredArgsConstructor public class WorkflowPolicyResolver {
 private final ScopeRuleMapper mapper;private final ObjectMapper json;private final WarningMapper materials;
 public record Effective(long id,int version,Policy policy){}
 public Effective resolve(InternshipPlacement p){var row=mapper.effective(p.getBatchId(),p.getStudentId());if(row==null)return null;try{return new Effective(((Number)row.get("id")).longValue(),((Number)row.get("version")).intValue(),json.readValue((String)row.get("settings"),Policy.class));}catch(java.io.IOException ex){throw BusinessException.badRequest("业务规则无效，请联系学校管理员");}}
 public void approval(InternshipPlacement p){var e=resolve(p);if(e==null)return;var r=e.policy();if(ChronoUnit.DAYS.between(p.getStartDate(),p.getEndDate())+1<r.minimumDays())throw BusinessException.badRequest("实习时长不足规则要求的 "+r.minimumDays()+" 天");if(r.approvalMaterials())required(p,r);}
 public void required(InternshipPlacement p,Policy r){for(var kind:r.requiredKinds())if(materials.material(p.getId(),kind)==0)throw BusinessException.badRequest("缺少规则要求的已审核材料："+kind);}
 public void report(InternshipPlacement p,java.time.LocalDate from,java.time.LocalDate to){var e=resolve(p);if(e==null||!e.policy().reports())return;long delta=ChronoUnit.DAYS.between(p.getStartDate(),from);var end=from.plusDays(e.policy().frequencyDays()-1);if(end.isAfter(p.getEndDate()))end=p.getEndDate();if(delta%e.policy().frequencyDays()!=0||!to.equals(end))throw BusinessException.badRequest("周报须按实习开始日起每 "+e.policy().frequencyDays()+" 天一期，最后一期截止实习结束日");}
}
