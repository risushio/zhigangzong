package com.zhigangzong;

import com.zhigangzong.dto.JobFilter;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.JobMatchingService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.time.LocalDate;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class MatchingIntegrationTest extends PortalTestFixture {
 @Test void favoritesAndFeedbackHaveIndependentOwnershipAndResolution()throws Exception {
  send(put("/api/portal/favorites/"+job),student,null,200);send(put("/api/portal/favorites/"+job),student,null,200);
  assertEquals(1,send(get("/api/portal/favorites"),student,null,200).path("total").asInt());
  assertEquals(0,send(get("/api/portal/favorites"),other,null,200).path("total").asInt());
  send(delete("/api/portal/favorites/"+job),other,null,200);
  assertEquals(1,send(get("/api/portal/favorites"),student,null,200).path("total").asInt());
  long id=send(post("/api/portal/feedbacks"),student,java.util.Map.of("jobId",job,"feedbackType","NOT_SUITABLE","reason","Time conflicts"),200).path("id").asLong();
  send(post("/api/portal/feedbacks/"+id+"/handle"),foreignAdmin,java.util.Map.of("note","No"),404);
  send(post("/api/portal/feedbacks/"+id+"/handle"),other,java.util.Map.of("note","No"),403);
  assertEquals(0,send(get("/api/portal/feedbacks"),other,null,200).path("total").asInt());
  send(post("/api/portal/feedbacks/"+id+"/handle"),admin,java.util.Map.of("note","Please revise availability"),200);
  send(post("/api/portal/feedbacks/"+id+"/handle"),admin,java.util.Map.of("note","Again"),400);
  assertEquals("Please revise availability",send(get("/api/portal/feedbacks"),student,null,200).path("items").get(0).path("resolution").asText());
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM match_feedback_resolution WHERE feedback_id=?",Integer.class,id));
  db.update("UPDATE job_position SET publish_status='OFFLINE' WHERE id=?",job);
  assertEquals(1,send(get("/api/portal/favorites"),student,null,200).path("total").asInt());
  send(put("/api/portal/favorites/"+job),student,null,400);
  send(delete("/api/portal/favorites/"+job),student,null,200);
  assertEquals(0,send(get("/api/portal/favorites"),student,null,200).path("total").asInt());
 }
 @Test void recommendationsAndFiltersRespectVisibilityAndOwnership()throws Exception {
  db.update("UPDATE student_profile SET major='CS',skills='Java,SQL',preferred_city='Shanghai',available_from='2027-01-01',available_to='2027-12-31',days_per_week=5 WHERE id=?",studentId);
  db.update("UPDATE job_position SET required_major='CS,Design',required_skills='Java,SQL',days_per_week=5 WHERE id=?",job);
  var list=send(get("/api/portal/recommendations"),student,null,200);
  var r=java.util.stream.StreamSupport.stream(list.spliterator(),false).filter(x->x.path("jobId").asLong()==job).findFirst().orElseThrow();
  assertEquals(100,r.path("score").asInt());assertEquals(0,r.path("conflicts").size());
  assertTrue(send(get("/api/recommendations").param("studentId",""+studentId),admin,null,200).isArray());
  send(get("/api/portal/recommendations").param("studentId",""+studentId),other,null,404);
  send(get("/api/portal/recommendations").param("studentId",""+studentId),foreignAdmin,null,404);
  send(get("/api/portal/recommendations"),recruiter,null,403);
  var filtered=send(get("/api/portal/jobs").param("q","Test Intern").param("major","CS").param("skills","Java,SQL").param("city","Shanghai").param("from","2027-03-01").param("to","2027-06-30"),student,null,200);
  assertTrue(java.util.stream.StreamSupport.stream(filtered.path("items").spliterator(),false).anyMatch(x->x.path("id").asLong()==job));
  assertEquals(0,send(get("/api/portal/jobs").param("q","Test Intern").param("skills","Rust"),student,null,200).path("total").asInt());
  db.update("UPDATE job_position SET required_skills='JavaScript,SQL' WHERE id=?",job);
  assertEquals(0,send(get("/api/portal/jobs").param("q","Test Intern").param("skills","Java"),student,null,200).path("total").asInt());
  send(get("/api/portal/jobs").param("from","2027-07-01").param("to","2027-01-01"),student,null,400);
  db.update("UPDATE job_position SET application_deadline='2020-01-01' WHERE id=?",job);
  assertFalse(java.util.stream.StreamSupport.stream(send(get("/api/portal/recommendations"),student,null,200).spliterator(),false).anyMatch(x->x.path("jobId").asLong()==job));
  db.update("UPDATE job_position SET application_deadline=NULL,publish_status='OFFLINE' WHERE id=?",job);
  assertEquals(0,send(get("/api/portal/jobs").param("q","Test Intern"),student,null,200).path("total").asInt());
 }
 @Test void scoreExplainsConflictsAndDoesNotMatchJavaScriptAsJava(){
  var s=new StudentProfile();s.setMajor("Design");s.setSkills("JavaScript");s.setPreferredCity("Beijing");s.setAvailableFrom(LocalDate.of(2027,3,1));s.setAvailableTo(LocalDate.of(2027,6,1));s.setDaysPerWeek(2);
  var j=new JobPosition();j.setId(1L);j.setTitle("Intern");j.setCity("Shanghai");j.setRequiredMajor("CS");j.setRequiredSkills("Java,SQL");j.setStartDate(LocalDate.of(2027,1,1));j.setEndDate(LocalDate.of(2027,12,31));j.setDaysPerWeek(5);
  var r=JobMatchingService.score(s,j);assertEquals(0,r.score().intValue());assertEquals(4,r.conflicts().size());assertEquals(java.util.List.of("java","sql"),r.missingSkills());
  s.setAvailableTo(null);assertTrue(JobMatchingService.score(s,j).missingInformation().contains("学生可实习起止日期不完整"));
  assertThrows(RuntimeException.class,()->new JobFilter("x".repeat(101),"","","",null,null));
 }
}
