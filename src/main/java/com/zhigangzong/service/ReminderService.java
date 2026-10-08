package com.zhigangzong.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.dto.WarningRequests.Policy;
import com.zhigangzong.mapper.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class ReminderService {
 private final PortalService portal;private final ReminderMapper mapper;private final WorkflowPolicyResolver rules;private final WarningMapper warnings;private final StudentProfileMapper students;private final ObjectMapper json;
 public Map<String,Object> scan(){return scanSchool(portal.actor("SCHOOL_ADMIN").schoolId());}
 public Map<String,Object> scanSchool(long school){mapper.lockSchool(school);var active=new HashSet<String>();var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));int created=0;
  for(var p:mapper.placements(school)){var effective=rules.resolve(p);List<String> kinds=List.of();int materialDays=0,frequency=7,grace=0;boolean reports=false;
   if(effective!=null){var r=effective.policy();kinds=r.requiredKinds();materialDays=r.materialDays();frequency=r.frequencyDays();grace=r.reportGraceDays();reports=r.reports();}
   else {String settings=warnings.policy(p.getBatchId());if(settings==null)continue;try{var r=json.readValue(settings,Policy.class);kinds=r.materials()?r.requiredKinds():List.of();materialDays=r.materialDays();frequency=r.frequencyDays();grace=r.reportGraceDays();reports=r.reports();}catch(java.io.IOException ex){throw BusinessException.badRequest("批次提醒规则无效");}}
   for(var kind:kinds)if(warnings.material(p.getId(),kind)==0)created+=remind(school,p,"MATERIAL:"+kind,p.getStartDate().plusDays(materialDays),"材料截止提醒："+kind,today,active);
   if(reports&&"ARRIVED".equals(p.getArrivalStatus())){var submitted=warnings.reports(p.getId());for(var from=p.getStartDate();!from.isAfter(p.getEndDate());from=from.plusDays(frequency)){var end=from.plusDays(frequency-1);if(end.isAfter(p.getEndDate()))end=p.getEndDate();var start=from;var to=end;if(submitted.stream().noneMatch(r->!r.getPeriodStart().isAfter(start)&&!r.getPeriodEnd().isBefore(to)))created+=remind(school,p,"REPORT:"+start,end.plusDays(grace),"周报截止提醒："+start+" 至 "+end,today,active);}}
  }
  for(var r:mapper.reminders(school))if(!active.contains((String)r.get("reminder_key")))mapper.resolved(((Number)r.get("notification_id")).longValue());
  return Map.of("created",created);
 }
 private int remind(long school,InternshipPlacement p,String type,LocalDate due,String title,LocalDate today,Set<String> active){String key=p.getId()+":"+type+":"+due;active.add(key);if(today.isBefore(due.minusDays(3))||mapper.exists(key)!=null)return 0;var row=new HashMap<String,Object>();mapper.notification(row,students.findById(p.getStudentId()).getUserId(),title,"截止日期 "+due+"；请在实习 #"+p.getId()+" 办理。已读与完成待办分别记录，完成待办不替代材料审核或周报提交。",p.getId(),due.atTime(23,59,59));mapper.link(key,school,p.getId(),((Number)row.get("id")).longValue());return 1;}
 public void complete(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR","RECRUITER");if(mapper.complete(id,a.id())==0)throw BusinessException.notFound("本人通知");}
}
