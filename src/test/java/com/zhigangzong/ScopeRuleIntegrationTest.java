package com.zhigangzong;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class ScopeRuleIntegrationTest extends PortalTestFixture {
 Map<String,Object> policy(int minimum,boolean attendance,boolean materials){return Map.of("minimumDays",minimum,"approvalMaterials",materials,"attendanceEnabled",attendance,"reports",true,"frequencyDays",7,"reportGraceDays",2,"materialDays",14,"requiredKinds",List.of("AGREEMENT"),"grading",Map.of("studentWeight",20,"enterpriseWeight",30,"teacherWeight",50,"passScore",60,"note","Scope test"));}
 void save(Long b,String major,Map<String,Object> p,String who,int status)throws Exception{var r=new HashMap<String,Object>();r.put("departmentId",department);r.put("batchId",b);r.put("major",major);r.put("policy",p);send(post("/api/portal/workflow-policies"),who,r,status);}
 @Test void rulesExecuteWithScopePriorityAndExistingOverrides()throws Exception {
  long app=apply();transition(app,recruiter,"OFFERED");transition(app,student,"ACCEPTED");long p=placement(app);
  save(null,"",policy(150,true,false),admin,200);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Too short"),400);
  save(null,"CS",policy(1,false,true),admin,200);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Missing material"),400);
  save(batch,"",policy(1,true,false),admin,200);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Batch priority"),200);
  assertTrue(send(get("/api/portal/placements/"+p+"/attendance"),student,null,200).path("enabled").asBoolean());
  send(put("/api/portal/placements/"+p+"/attendance-policy"),admin,Map.of("enabled",false,"note","Placement override"),200);
  assertFalse(send(get("/api/portal/placements/"+p+"/attendance"),student,null,200).path("enabled").asBoolean());
  db.update("UPDATE internship_placement SET arrival_status='ARRIVED' WHERE id=?",p);
  var report=Map.of("periodStart","2027-03-01","periodEnd","2027-03-06","title","Report","content","Test content");
  send(post("/api/portal/placements/"+p+"/reports"),student,report,400);
  send(post("/api/portal/placements/"+p+"/reports"),student,Map.of("periodStart","2027-03-01","periodEnd","2027-03-07","title","Report","content","Test content"),200);
  save(batch,"CS",policy(1,false,false),admin,200);
  var effective=send(get("/api/portal/placements/"+p+"/workflow-policy"),student,null,200).path("rule");assertEquals(1,effective.path("version").asInt());
  save(batch,"CS",policy(2,false,false),admin,200);
  assertEquals(2,send(get("/api/portal/placements/"+p+"/workflow-policy"),student,null,200).path("rule").path("version").asInt());
  send(get("/api/portal/placements/"+p+"/workflow-policy"),other,null,404);save(batch,"",policy(1,true,false),foreignAdmin,404);save(batch,"",policy(1,true,false),student,403);
  long teacher=userId(account(school,department,"TEACHER")),mentor=userId(account(school,null,"ENTERPRISE_MENTOR"));
  db.update("UPDATE internship_placement SET teacher_id=?,enterprise_mentor_id=? WHERE id=?",teacher,mentor,p);
  for(var item:List.of(new Object[]{"STUDENT",userId(student),80},new Object[]{"TEACHER",teacher,90},new Object[]{"ENTERPRISE",mentor,100}))db.update("INSERT INTO evaluation_entry(placement_id,evaluator_id,type,status,score,comment,revision) VALUES(?,?,?,'SUBMITTED',?,'Test',1)",p,item[1],item[0],item[2]);
  var calc=send(get("/api/portal/placements/"+p+"/evaluations"),student,null,200).path("calculation");assertEquals(91,calc.path("total").asInt());assertEquals(2,calc.path("policyVersion").asInt());assertEquals(effective.path("id").asLong(),calc.path("workflowPolicyId").asLong());
  send(put("/api/portal/batches/"+batch+"/grading"),admin,Map.of("studentWeight",100,"teacherWeight",0,"enterpriseWeight",0,"passScore",60,"note","Batch grading override"),200);
  assertEquals(80,send(get("/api/portal/placements/"+p+"/evaluations"),student,null,200).path("calculation").path("total").asInt());
 }
}
