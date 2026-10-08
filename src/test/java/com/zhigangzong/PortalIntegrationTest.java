package com.zhigangzong;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.bootstrap.enabled=false") @AutoConfigureMockMvc @Transactional
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class PortalIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate db;
 String student,recruiter,admin,other,foreignAdmin,otherRecruiter; long school,department,studentId,enterprise,job,batch;
 long insert(String sql,Object... args){db.update(sql,args);return db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 String account(long school,Long dept,String role){
  String name="it-"+UUID.randomUUID().toString().substring(0,18);
  long id=insert("INSERT INTO user_account(school_id,department_id,display_name,role) VALUES(?,?,?,?)",school,dept,name,role);
  db.update("INSERT INTO auth_account(user_id,username,password_hash) VALUES(?,?,'unused-test-hash')",id,name);
  return name;
 }
 long userId(String name){return db.queryForObject("SELECT user_id FROM auth_account WHERE username=?",Long.class,name);}
 @BeforeEach void setup(){
  school=insert("INSERT INTO school(name,code) VALUES('Portal Test',?)","IT-"+UUID.randomUUID().toString().substring(0,20));
  department=insert("INSERT INTO department(school_id,name,code) VALUES(?,'Engineering','ENG')",school);
  student=account(school,department,"STUDENT"); other=account(school,department,"STUDENT");admin=account(school,null,"SCHOOL_ADMIN");recruiter=account(school,null,"RECRUITER");otherRecruiter=account(school,null,"RECRUITER");
  studentId=insert("INSERT INTO student_profile(user_id,student_no,major) VALUES(?,'001','CS')",userId(student));
  db.update("INSERT INTO student_profile(user_id,student_no,major) VALUES(?,'002','CS')",userId(other));
  enterprise=insert("INSERT INTO enterprise(name,credit_code,review_status) VALUES('IT Enterprise',?,'APPROVED')","IT-"+UUID.randomUUID().toString().substring(0,20));
  db.update("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(?,?)",userId(recruiter),enterprise);
  long second=insert("INSERT INTO enterprise(name,credit_code) VALUES('Other',?)","IT-"+UUID.randomUUID().toString().substring(0,20));
  db.update("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(?,?)",userId(otherRecruiter),second);
  job=insert("INSERT INTO job_position(enterprise_id,title,description,city,headcount,review_status,publish_status,start_date,end_date) VALUES(?,'Test Intern','Job','Shanghai',2,'APPROVED','PUBLISHED','2027-01-01','2027-12-31')",enterprise);
  batch=insert("INSERT INTO internship_batch(department_id,name,start_date,end_date) VALUES(?,'2027 batch','2027-01-01','2027-12-31')",department);
  long otherSchool=insert("INSERT INTO school(name,code) VALUES('Foreign',?)","IT-"+UUID.randomUUID().toString().substring(0,20));
  foreignAdmin=account(otherSchool,null,"SCHOOL_ADMIN");
 }
 String role(String name){return db.queryForObject("SELECT role FROM user_account WHERE id=?",String.class,userId(name));}
 MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder r,String name){return r.with(user(name).roles(role(name))).with(csrf());}
 JsonNode send(MockHttpServletRequestBuilder r,String name,Object body,int expected)throws Exception{
  if(body!=null)r.contentType("application/json").content(json.writeValueAsString(body));
  return json.readTree(mvc.perform(as(r,name)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString()).path("data");
 }
 long apply()throws Exception{return send(post("/api/portal/applications"),student,Map.of("jobId",job,"resumeRef","test-resume"),200).path("id").asLong();}
 void transition(long id,String who,String status)throws Exception{send(post("/api/portal/applications/"+id+"/transition"),who,Map.of("status",status,"note","Integration test","interviewAt","2027-02-01T10:00:00","offerDetails","Offer details"),200);}
 long placement(long app)throws Exception{return send(post("/api/portal/placements"),student,Map.of("applicationId",app,"batchId",batch,"startDate","2027-03-01","endDate","2027-06-30"),200).path("id").asLong();}
 @Test void fullWorkflowKeepsSchoolApprovalIndependent()throws Exception{
  long app=apply();transition(app,recruiter,"INTERVIEW");transition(app,recruiter,"OFFERED");
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM internship_placement WHERE application_id=?",Integer.class,app));
  send(post("/api/portal/placements"),student,Map.of("applicationId",app,"batchId",batch,"startDate","2027-03-01","endDate","2027-06-30"),400);
  transition(app,student,"ACCEPTED");long p=placement(app);
  assertEquals("PENDING",db.queryForObject("SELECT school_approval_status FROM internship_placement WHERE id=?",String.class,p));
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","RETURNED","comment","Fix dates"),200);
  send(post("/api/portal/placements/"+p+"/resubmit"),student,Map.of("applicationId",app,"batchId",batch,"startDate","2027-03-02","endDate","2027-06-30"),200);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Approved"),200);
  assertEquals("ACCEPTED",db.queryForObject("SELECT recruitment_status FROM job_application WHERE id=?",String.class,app));
  assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM approval_record WHERE placement_id=?",Integer.class,p));
  assertEquals(4,send(get("/api/portal/applications/"+app),student,null,200).path("events").size());
  assertTrue(send(get("/api/portal/notifications"),student,null,200).path("total").asInt()>0);
  assertTrue(send(get("/api/portal/notifications"),recruiter,null,200).path("total").asInt()>0);
  assertTrue(send(get("/api/portal/notifications"),admin,null,200).path("total").asInt()>0);
  assertEquals(0,send(get("/api/portal/notifications"),otherRecruiter,null,200).path("total").asInt());
  assertEquals(0,send(get("/api/portal/notifications"),foreignAdmin,null,200).path("total").asInt());
 }
 @Test void ownershipAndRoleBoundariesCannotBeBypassed()throws Exception{
  long app=apply();
  send(get("/api/portal/applications/"+app),other,null,404);
  send(get("/api/portal/applications/"+app),otherRecruiter,null,404);
  send(get("/api/portal/applications/"+app),foreignAdmin,null,404);
  assertEquals(0,send(get("/api/portal/applications"),other,null,200).path("total").asInt());
  assertEquals(0,send(get("/api/portal/applications"),otherRecruiter,null,200).path("total").asInt());
  send(get("/api/students"),student,null,403);
  send(post("/api/portal/applications/"+app+"/transition"),student,Map.of("status","OFFERED","note","Tamper"),400);
  send(post("/api/portal/applications/"+app+"/transition"),recruiter,Map.of("status","ACCEPTED","note","Tamper"),400);
  transition(app,recruiter,"OFFERED");transition(app,student,"ACCEPTED");long p=placement(app);
  send(post("/api/portal/placements/"+p+"/approval"),student,Map.of("decision","APPROVED","comment","Tamper"),403);
  send(post("/api/portal/placements/"+p+"/approval"),foreignAdmin,Map.of("decision","APPROVED","comment","Tamper"),404);
  send(get("/api/portal/placements/"+p),other,null,404);
  send(get("/api/portal/placements"),recruiter,null,403);
 }
 @Test void duplicateAndInvalidTransitionsDoNotWriteHistory()throws Exception{
  long app=apply();send(post("/api/portal/applications"),student,Map.of("jobId",job,"resumeRef","again"),409);
  send(post("/api/portal/applications/"+app+"/transition"),recruiter,Map.of("status","INTERVIEW","note","Missing date"),400);
  transition(app,student,"WITHDRAWN");send(post("/api/portal/applications/"+app+"/transition"),recruiter,Map.of("status","OFFERED","note","Too late","offerDetails","Offer"),400);
  assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM recruitment_event WHERE application_id=?",Integer.class,app));
  db.update("UPDATE job_position SET publish_status='OFFLINE' WHERE id=?",job);
  send(post("/api/portal/applications"),other,Map.of("jobId",job,"resumeRef","Unavailable"),400);
 }
 @Test void profileEditAndNotificationOwnership()throws Exception{
  send(put("/api/portal/profile"),student,Map.of("major","Software","skills","Java","daysPerWeek",5),200);
  assertEquals("Software",send(get("/api/portal/profile"),student,null,200).path("major").asText());
  assertEquals("CS",send(get("/api/portal/profile"),other,null,200).path("major").asText());
  send(put("/api/portal/profile"),student,Map.of("major","Software","availableFrom","2027-06-01","availableTo","2027-01-01"),400);
  long n=insert("INSERT INTO notification(recipient_id,title) VALUES(?,'Private')",userId(student));
  send(post("/api/portal/notifications/"+n+"/read"),other,null,404);
  send(post("/api/portal/notifications/"+n+"/read"),student,null,200);
 }
 @Test void accountProvisionRequiresLocalAdminAndDoesNotOverwrite()throws Exception{
  long id=insert("INSERT INTO user_account(school_id,department_id,display_name,role) VALUES(?,?,'New Student','STUDENT')",school,department);
  db.update("INSERT INTO student_profile(user_id,student_no,major) VALUES(?,'new','CS')",id);
  var body=Map.of("userId",id,"username","new-"+UUID.randomUUID().toString().substring(0,20),"password","integration-password");
  send(post("/api/portal/accounts"),student,body,403);
  send(post("/api/portal/accounts"),foreignAdmin,body,404);
  send(post("/api/portal/accounts"),admin,body,200);
  send(post("/api/portal/accounts"),admin,body,409);
  assertTrue(db.queryForObject("SELECT password_hash FROM auth_account WHERE user_id=?",String.class,id).startsWith("$2"));
 }
 @Test void existingApplicationsCannotBeMovedToAnotherRecruiter()throws Exception{
  long app=apply();
  long target=db.queryForObject("SELECT enterprise_id FROM enterprise_member WHERE user_id=?",Long.class,userId(otherRecruiter));
  send(put("/api/jobs/"+job),admin,Map.of("enterpriseId",target,"title","Moved","description","Should not move","city","Shanghai","headcount",2),400);
  send(get("/api/portal/applications/"+app),otherRecruiter,null,404);
  send(get("/api/portal/applications/"+app),recruiter,null,200);
 }

 String teacher,mentor,outsider;
 long processPlacement() {
  teacher=account(school,department,"TEACHER");mentor=account(school,null,"ENTERPRISE_MENTOR");outsider=account(school,department,"TEACHER");
  db.update("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(?,?)",userId(mentor),enterprise);
  return insert("INSERT INTO internship_placement(student_id,batch_id,source,enterprise_id,position_title,school_approval_status,start_date,end_date) VALUES(?,?,'SELF',?,'Process Test','APPROVED',CURRENT_DATE-INTERVAL 7 DAY,CURRENT_DATE+INTERVAL 60 DAY)",studentId,batch,enterprise);
 }
 Map<String,Object> attendanceBody(String type,int days){return Map.of("recordType",type,"attendanceDate",java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(days).toString(),"note","Attendance test reason");}
 void enableAttendance(long p)throws Exception{send(put("/api/portal/placements/"+p+"/attendance-policy"),admin,Map.of("enabled",true,"note","Internship requires attendance"),200);}
 @Test void attendanceRoundTripPreservesReasonsAndReviewHistory()throws Exception{
  long p=processPlacement();assign(p);arrive(p);String base="/api/portal/placements/"+p+"/attendance";
  assertFalse(send(get(base),student,null,200).path("enabled").asBoolean());
  send(post(base),student,attendanceBody("CHECK_IN",0),400);enableAttendance(p);
  long check=send(post(base),student,attendanceBody("CHECK_IN",0),200).path("id").asLong();
  assertEquals("APPROVED",send(get("/api/portal/attendance/"+check),student,null,200).path("record").path("status").asText());
  send(post(base),student,attendanceBody("CHECK_IN",0),400);
  long leave=send(post(base),student,attendanceBody("LEAVE",1),200).path("id").asLong();
  String url="/api/portal/attendance/"+leave;
  send(post(url+"/review"),teacher,Map.of("decision","RETURNED","feedback","Add appointment evidence"),200);
  send(post(url+"/resubmit"),student,Map.of("note","Appointment evidence added"),200);
  send(post(url+"/review"),teacher,Map.of("decision","APPROVED","feedback","Appointment verified"),200);
  var history=send(get(url),mentor,null,200).path("events");assertEquals(4,history.size());
  assertEquals("Attendance test reason",history.get(0).path("note").asText());
  assertEquals("Add appointment evidence",history.get(1).path("feedback").asText());
  assertEquals("Appointment evidence added",history.get(2).path("note").asText());
  long makeup=send(post(base),student,attendanceBody("MAKE_UP",-1),200).path("id").asLong();
  send(post("/api/portal/attendance/"+makeup+"/review"),teacher,Map.of("decision","APPROVED","feedback","Verified missed check-in"),200);
  assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM attendance_record WHERE placement_id=? AND status='APPROVED'",Integer.class,p));
  assertEquals(7,db.queryForObject("SELECT COUNT(*) FROM attendance_record_event h JOIN attendance_record r ON r.id=h.record_id WHERE r.placement_id=?",Integer.class,p));
  send(post(url+"/resubmit"),student,Map.of("note","Already approved"),400);
  send(post(url+"/review"),teacher,Map.of("decision","RETURNED","feedback","Already approved"),400);
 }
 @Test void attendanceRejectsInvalidDatesAndUnauthorizedActors()throws Exception{
  long p=processPlacement();assign(p);arrive(p);enableAttendance(p);String base="/api/portal/placements/"+p+"/attendance";
  for(var body:List.of(attendanceBody("CHECK_IN",-1),attendanceBody("CHECK_IN",1),attendanceBody("MAKE_UP",0),attendanceBody("MAKE_UP",1),attendanceBody("LEAVE",61),attendanceBody("MAKE_UP",-8)))send(post(base),student,body,400);
  send(post(base),student,Map.of("recordType","APPEAL","attendanceDate",java.time.LocalDate.now().toString(),"note","Unsupported"),400);
  send(post(base),student,Map.of("recordType","LEAVE","attendanceDate",java.time.LocalDate.now().toString(),"note"," "),400);
  send(put(base+"-policy"),student,Map.of("enabled",false,"note","Unauthorized"),403);
  send(put(base+"-policy"),foreignAdmin,Map.of("enabled",false,"note","Wrong school"),404);
  for(String who:List.of(other,outsider,foreignAdmin))send(get(base),who,null,404);
  send(get(base),recruiter,null,403);
  long r=send(post(base),student,attendanceBody("MAKE_UP",-1),200).path("id").asLong();String url="/api/portal/attendance/"+r;
  send(get(url),other,null,404);send(get(url),outsider,null,404);
  for(String who:List.of(student,mentor,admin))send(post(url+"/review"),who,Map.of("decision","APPROVED","feedback","Wrong role"),403);
  send(post(url+"/review"),outsider,Map.of("decision","APPROVED","feedback","Wrong teacher"),404);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Reassign attendance reviewer"),200);
  send(get(url),teacher,null,404);send(post(url+"/review"),teacher,Map.of("decision","APPROVED","feedback","Former teacher"),404);
  send(post(url+"/review"),outsider,Map.of("decision","APPROVED","feedback","Current teacher"),200);
 }
 @Test void attendancePolicyDisablingPreservesHistoryAndPendingReview()throws Exception{
  long p=processPlacement();assign(p);enableAttendance(p);String base="/api/portal/placements/"+p+"/attendance";
  send(post(base),student,attendanceBody("CHECK_IN",0),400);arrive(p);
  long r=send(post(base),student,attendanceBody("LEAVE",1),200).path("id").asLong();String url="/api/portal/attendance/"+r;
  send(put(base+"-policy"),admin,Map.of("enabled",false,"note","Attendance no longer required"),200);
  send(post(base),student,attendanceBody("MAKE_UP",-1),400);
  send(post(url+"/review"),teacher,Map.of("decision","RETURNED","feedback","Needs evidence"),200);
  send(post(url+"/resubmit"),student,Map.of("note","Evidence"),400);enableAttendance(p);
  send(post(base),student,attendanceBody("LEAVE",1),400);
  send(post(url+"/resubmit"),student,Map.of("note","Evidence"),200);
  send(post(url+"/review"),teacher,Map.of("decision","REJECTED","feedback","Unable to verify"),200);
  send(post(base),student,attendanceBody("LEAVE",1),200);
  assertEquals(3,send(get(base),student,null,200).path("policyEvents").size());
  assertEquals(4,send(get(url),student,null,200).path("events").size());
 }
 Map<String,Object> extensionBody(int days){return Map.of("endDate",java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(days).toString(),"reason","Extension integration arrangement");}
 String placementEnd(long p){return db.queryForObject("SELECT CAST(end_date AS CHAR) FROM internship_placement WHERE id=?",String.class,p);}
 @Test void extensionApprovalUpdatesOnlyEndAndPreservesEachSnapshot()throws Exception{
  long p=processPlacement();assign(p);arrive(p);String base="/api/portal/placements/"+p+"/extensions",original=placementEnd(p);
  long r=send(post(base),student,extensionBody(70),200).path("id").asLong();String url="/api/portal/extensions/"+r;
  assertEquals(original,placementEnd(p));
  send(post(base),student,extensionBody(80),400);
  send(post(url+"/review"),admin,Map.of("decision","RETURNED","comment","Clarify extension arrangement"),200);
  assertEquals(original,placementEnd(p));
  var amended=new HashMap<>(extensionBody(75));amended.put("reason","Clarified and verified extension arrangement");
  send(post(url+"/resubmit"),student,amended,200);
  send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Extension arrangement approved"),200);
  assertEquals(amended.get("endDate"),placementEnd(p));
  var d=send(get(url),mentor,null,200);assertEquals(4,d.path("events").size());
  assertEquals(original,json.readTree(d.path("request").path("originalSnapshot").asText()).path("endDate").asText());
  assertEquals(extensionBody(70).get("endDate"),json.readTree(d.path("events").get(0).path("requested_snapshot").asText()).path("endDate").asText());
  assertEquals("Clarify extension arrangement",d.path("events").get(1).path("review_comment").asText());
  assertEquals("ARRIVED",db.queryForObject("SELECT arrival_status FROM internship_placement WHERE id=?",String.class,p));
  assertEquals(userId(teacher),db.queryForObject("SELECT teacher_id FROM internship_placement WHERE id=?",Long.class,p));
  assertEquals(userId(mentor),db.queryForObject("SELECT enterprise_mentor_id FROM internship_placement WHERE id=?",Long.class,p));
  enableAttendance(p);
  send(post("/api/portal/placements/"+p+"/attendance"),student,attendanceBody("LEAVE",74),200);
  var report=new HashMap<>(reportBody());report.put("periodStart",java.time.LocalDate.now().plusDays(61).toString());report.put("periodEnd",java.time.LocalDate.now().plusDays(74).toString());
  send(post("/api/portal/placements/"+p+"/reports"),student,report,200);
  send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Duplicate approval"),400);
  send(post(url+"/resubmit"),student,extensionBody(80),400);
  long second=send(post(base),student,extensionBody(90),200).path("id").asLong();
  assertEquals(amended.get("endDate"),json.readTree(send(get("/api/portal/extensions/"+second),teacher,null,200).path("request").path("originalSnapshot").asText()).path("endDate").asText());
 }
 @Test void extensionBoundariesAndInvalidRequestsAreRejected()throws Exception{
  long p=processPlacement();assign(p);String base="/api/portal/placements/"+p+"/extensions";
  for(int days:List.of(59,60,-1))send(post(base),student,extensionBody(days),400);
  send(post(base),student,Map.of("endDate",extensionBody(70).get("endDate"),"reason"," "),400);
  send(post(base),teacher,extensionBody(70),403);send(post(base),mentor,extensionBody(70),403);
  send(post(base),other,extensionBody(70),404);
  db.update("UPDATE internship_placement SET school_approval_status='RETURNED' WHERE id=?",p);
  send(post(base),student,extensionBody(70),400);db.update("UPDATE internship_placement SET school_approval_status='APPROVED' WHERE id=?",p);
  long r=send(post(base),student,extensionBody(70),200).path("id").asLong();String url="/api/portal/extensions/"+r;
  for(String who:List.of(other,outsider,foreignAdmin))send(get(url),who,null,404);
  send(get(url),recruiter,null,403);
  for(String who:List.of(student,teacher,mentor))send(post(url+"/review"),who,Map.of("decision","APPROVED","comment","Wrong role"),403);
  send(post(url+"/review"),foreignAdmin,Map.of("decision","APPROVED","comment","Wrong school"),404);
  send(post(url+"/resubmit"),student,extensionBody(80),400);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Change guidance"),200);
  send(get(url),teacher,null,404);send(get(url),outsider,null,200);
 }
 @Test void extensionRejectAndStaleApprovalNeverChangePlacementDates()throws Exception{
  long p=processPlacement();String base="/api/portal/placements/"+p+"/extensions",original=placementEnd(p);
  long r=send(post(base),student,extensionBody(70),200).path("id").asLong();String url="/api/portal/extensions/"+r;
  send(post(url+"/review"),admin,Map.of("decision","REJECTED","comment","Not suitable"),200);assertEquals(original,placementEnd(p));
  send(post(url+"/resubmit"),student,extensionBody(75),400);
  long next=send(post(base),student,extensionBody(80),200).path("id").asLong();String nextUrl="/api/portal/extensions/"+next;
  db.update("UPDATE internship_placement SET end_date=DATE_ADD(end_date,INTERVAL 1 DAY) WHERE id=?",p);String changed=placementEnd(p);
  send(post(nextUrl+"/review"),admin,Map.of("decision","APPROVED","comment","Stale dates"),400);
  assertEquals(changed,placementEnd(p));assertEquals(1,send(get(nextUrl),student,null,200).path("events").size());
  send(post(nextUrl+"/review"),admin,Map.of("decision","REJECTED","comment","Original dates changed; apply again"),200);
  assertEquals(changed,placementEnd(p));send(post(base),student,extensionBody(90),200);
 }
 Map<String,Object> reportBody(){return Map.of("title","First week","periodStart",java.time.LocalDate.now().minusDays(6).toString(),"periodEnd",java.time.LocalDate.now().toString(),"content","Implemented and verified the internship task.");}
 void assign(long p)throws Exception{send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(teacher),"enterpriseMentorId",userId(mentor),"note","Assign supervisors"),200);}
 void arrive(long p)throws Exception{send(post("/api/portal/placements/"+p+"/arrival"),mentor,Map.of("arrivalDate",java.time.LocalDate.now().minusDays(7).toString(),"note","Verified arrival"),200);}
 @Test void processRoundTripPreservesSnapshotsAndFeedback()throws Exception{
  long p=processPlacement();assign(p);arrive(p);
  long r=send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),200).path("id").asLong();
  assertEquals("DRAFT",send(get("/api/portal/reports/"+r),student,null,200).path("report").path("status").asText());
  send(get("/api/portal/reports/"+r),teacher,null,404);
  assertEquals(0,send(get("/api/portal/placements/"+p+"/process"),teacher,null,200).path("reports").size());
  send(post("/api/portal/reports/"+r+"/submit"),student,null,200);
  send(put("/api/portal/reports/"+r),student,reportBody(),400);
  send(post("/api/portal/reports/"+r+"/review"),teacher,Map.of("decision","RETURNED","feedback","Add evidence"),200);
  var changed=new HashMap<>(reportBody());changed.put("content","Added verified task evidence.");
  send(put("/api/portal/reports/"+r),student,changed,200);
  send(post("/api/portal/reports/"+r+"/submit"),student,null,200);
  send(post("/api/portal/reports/"+r+"/review"),teacher,Map.of("decision","REVIEWED","feedback","Evidence checked"),200);
  assertEquals("REVIEWED",db.queryForObject("SELECT status FROM progress_report WHERE id=?",String.class,r));
  assertEquals(6,db.queryForObject("SELECT COUNT(*) FROM progress_report_event WHERE report_id=?",Integer.class,r));
  assertEquals("Add evidence",db.queryForObject("SELECT feedback FROM progress_report_event WHERE report_id=? AND action='RETURNED'",String.class,r));
  assertTrue(send(get("/api/portal/notifications"),teacher,null,200).path("total").asInt()>=2);
 }
 @Test void processAccessRequiresCurrentGuidanceAndCompany()throws Exception{
  long p=processPlacement();assign(p);arrive(p);
  long r=send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),200).path("id").asLong();send(post("/api/portal/reports/"+r+"/submit"),student,null,200);
  for(String who:List.of(outsider,other,foreignAdmin,otherRecruiter))send(get("/api/portal/placements/"+p+"/process"),who,null,who.equals(otherRecruiter)?403:404);
  send(get("/api/portal/reports/"+r),outsider,null,404);
  send(post("/api/portal/reports/"+r+"/review"),mentor,Map.of("decision","REVIEWED","feedback","No teacher authority"),403);
  send(get("/api/students"),teacher,null,403);
  assertEquals(0,send(get("/api/portal/placements"),outsider,null,200).path("total").asInt());
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Reassign"),200);
  send(get("/api/portal/reports/"+r),teacher,null,404);send(get("/api/portal/reports/"+r),outsider,null,200);
  long second=db.queryForObject("SELECT enterprise_id FROM enterprise_member WHERE user_id=?",Long.class,userId(otherRecruiter));
  db.update("UPDATE enterprise_member SET enterprise_id=? WHERE user_id=?",second,userId(mentor));
  send(get("/api/portal/placements/"+p+"/process"),mentor,null,404);
  assertEquals(0,send(get("/api/portal/placements"),mentor,null,200).path("total").asInt());
 }
 @Test void arrivalAndReportsRejectInvalidOrderAndDates()throws Exception{
  long p=processPlacement();
  send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),400);
  send(post("/api/portal/placements/"+p+"/arrival"),student,Map.of("arrivalDate",java.time.LocalDate.now().toString(),"note","No permission"),403);
  assign(p);
  send(post("/api/portal/placements/"+p+"/arrival"),mentor,Map.of("arrivalDate",java.time.LocalDate.now().plusDays(1).toString(),"note","Future"),400);
  arrive(p);send(post("/api/portal/placements/"+p+"/arrival"),mentor,Map.of("arrivalDate",java.time.LocalDate.now().toString(),"note","Duplicate"),400);
  var invalid=new HashMap<>(reportBody());invalid.put("periodEnd",java.time.LocalDate.now().minusDays(10).toString());
  send(post("/api/portal/placements/"+p+"/reports"),student,invalid,400);
  long r=send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),200).path("id").asLong();
  send(post("/api/portal/reports/"+r+"/review"),teacher,Map.of("decision","REVIEWED","feedback","Not submitted"),404);
  send(post("/api/portal/reports/"+r+"/submit"),student,null,200);send(post("/api/portal/reports/"+r+"/submit"),student,null,400);
  send(post("/api/portal/reports/"+r+"/review"),teacher,Map.of("decision","REVIEWED","feedback","OK"),200);
  send(post("/api/portal/reports/"+r+"/review"),teacher,Map.of("decision","RETURNED","feedback","Duplicate"),400);
  assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM progress_report_event WHERE report_id=?",Integer.class,r));
 }
 @Test void assignmentRejectsForeignSchoolCompanyAndUnapproved()throws Exception{
  long p=processPlacement();
  String foreignTeacher=account(db.queryForObject("SELECT school_id FROM user_account WHERE id=?",Long.class,userId(foreignAdmin)),null,"TEACHER");
  String wrongMentor=account(school,null,"ENTERPRISE_MENTOR");
  db.update("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(?,?)",userId(wrongMentor),db.queryForObject("SELECT enterprise_id FROM enterprise_member WHERE user_id=?",Long.class,userId(otherRecruiter)));
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(teacher),"enterpriseMentorId",userId(wrongMentor),"note","Wrong company"),400);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(foreignTeacher),"enterpriseMentorId",userId(mentor),"note","Wrong school"),400);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(teacher),"enterpriseMentorId",userId(otherRecruiter),"note","Wrong role"),400);
  db.update("UPDATE internship_placement SET school_approval_status='PENDING' WHERE id=?",p);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(teacher),"enterpriseMentorId",userId(mentor),"note","Not approved"),400);
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM placement_process_event WHERE placement_id=?",Integer.class,p));
 }
 @Test void teacherAndEnterpriseMentorAccountsAreProvisionedWithCorrectBinding()throws Exception{
  long t=insert("INSERT INTO user_account(school_id,display_name,role) VALUES(?,'New Teacher','TEACHER')",school);
  long m=insert("INSERT INTO user_account(school_id,display_name,role) VALUES(?,'New Mentor','ENTERPRISE_MENTOR')",school);
  String login="it-new-"+UUID.randomUUID().toString().substring(0,20);
  send(post("/api/portal/accounts"),admin,Map.of("userId",t,"username",login,"password","integration-password","enterpriseId",enterprise),400);
  send(post("/api/portal/accounts"),admin,Map.of("userId",t,"username",login,"password","integration-password"),200);
  send(post("/api/portal/accounts"),admin,Map.of("userId",m,"username",login+"m","password","integration-password"),400);
  send(post("/api/portal/accounts"),admin,Map.of("userId",m,"username",login+"m","password","integration-password","enterpriseId",enterprise),200);
  assertEquals(enterprise,db.queryForObject("SELECT enterprise_id FROM enterprise_member WHERE user_id=?",Long.class,m));
  send(post("/api/portal/accounts"),admin,Map.of("userId",m,"username",login+"m","password","integration-password","enterpriseId",enterprise),409);
 }

 Map<String,Object> selfBody(){return new HashMap<>(Map.of("batchId",batch,"enterpriseName","Self Unit","creditCode","SELF-"+UUID.randomUUID().toString().substring(0,20).toUpperCase(),"contactName","Demo contact","contactPhone","000-DEMO","address","Demo address","positionTitle","Self Intern","duties","Develop and verify services","startDate","2027-03-01","endDate","2027-06-30"));}
 @Test void selfPlacementDraftReturnResubmitKeepsUnitSnapshots()throws Exception{
  var body=selfBody();long p=send(post("/api/portal/self-placements"),student,body,200).path("id").asLong();
  assertEquals("DRAFT",db.queryForObject("SELECT school_approval_status FROM internship_placement WHERE id=?",String.class,p));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM job_application WHERE student_id=?",Integer.class,studentId));
  send(post("/api/portal/self-placements/"+p+"/submit"),student,null,200);
  send(put("/api/portal/self-placements/"+p),student,body,400);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","RETURNED","comment","Clarify address"),200);
  body.put("address","Updated verified demo address");send(put("/api/portal/self-placements/"+p),student,body,200);
  send(post("/api/portal/self-placements/"+p+"/submit"),student,null,200);
  send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Verified"),200);
  var detail=send(get("/api/portal/placements/"+p),student,null,200);
  assertEquals("Updated verified demo address",detail.path("selfDeclaration").path("address").asText());assertEquals(4,detail.path("selfHistory").size());assertEquals(2,detail.path("approvals").size());
  assertEquals("Demo address",db.queryForObject("SELECT address FROM self_placement_event WHERE placement_id=? ORDER BY id LIMIT 1",String.class,p));
  send(put("/api/portal/self-placements/"+p),student,body,400);
 }
 @Test void selfPlacementChecksScopeDatesDuplicatesAndDoesNotOverwriteCompany()throws Exception{
  var body=selfBody();body.put("enterpriseName","IT Enterprise");body.put("creditCode",db.queryForObject("SELECT credit_code FROM enterprise WHERE id=?",String.class,enterprise).toUpperCase());
  db.update("UPDATE enterprise SET credit_code=? WHERE id=?",body.get("creditCode"),enterprise);
  send(post("/api/portal/self-placements"),recruiter,body,403);
  var invalid=new HashMap<>(body);invalid.put("startDate","2026-01-01");send(post("/api/portal/self-placements"),student,invalid,400);
  long p=send(post("/api/portal/self-placements"),student,body,200).path("id").asLong();
  assertNull(db.queryForObject("SELECT contact_phone FROM enterprise WHERE id=?",String.class,enterprise));
  send(post("/api/portal/self-placements"),student,body,400);
  send(put("/api/portal/self-placements/"+p),other,body,404);
  send(get("/api/portal/placements/"+p),foreignAdmin,null,404);
  send(post("/api/portal/self-placements/"+p+"/submit"),other,null,404);
  db.update("UPDATE enterprise SET review_status='SUSPENDED' WHERE id=?",enterprise);
  send(post("/api/portal/self-placements/"+p+"/submit"),student,null,400);
 }
 long upload(String who,String kind,Long placement,String name,byte[] bytes,int expected)throws Exception{
  var req=multipart("/api/portal/files").file(new org.springframework.mock.web.MockMultipartFile("file",name,"application/octet-stream",bytes)).param("kind",kind);
  if(placement!=null)req.param("placementId",placement.toString());
  return send(req,who,null,expected).path("id").asLong();
 }
 @Test void fileBytesReviewAndGuidancePermissionsAreEnforced()throws Exception{
  long p=processPlacement();assign(p);arrive(p);
  byte[] bytes="Demo material, UTF-8 contents.".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  long f=upload(student,"AGREEMENT",p,"agreement.txt",bytes,200);
  var metadata=send(get("/api/portal/files/"+f),student,null,200).path("file");assertFalse(metadata.has("storageKey"));assertEquals(bytes.length,metadata.path("byteSize").asInt());
  mvc.perform(as(get("/api/portal/files/"+f+"/download"),teacher)).andExpect(status().isOk()).andExpect(content().bytes(bytes)).andExpect(header().string("Cache-Control","no-store, private"));
  mvc.perform(as(get("/api/portal/files/"+f+"/preview"),mentor)).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff"));
  for(String who:List.of(other,outsider,foreignAdmin,recruiter))send(get("/api/portal/files/"+f+"/download"),who,null,404);
  send(post("/api/portal/files/"+f+"/review"),mentor,Map.of("decision","APPROVED","comment","No authority"),403);
  send(post("/api/portal/files/"+f+"/review"),teacher,Map.of("decision","RETURNED","comment","Supplement required"),200);
  long second=upload(student,"AGREEMENT",p,"agreement-v2.txt",bytes,200);
  send(post("/api/portal/files/"+second+"/review"),admin,Map.of("decision","APPROVED","comment","Verified"),200);
  assertEquals(2,send(get("/api/portal/files?placementId="+p),student,null,200).path("total").asInt());
  assertEquals("RETURNED",db.queryForObject("SELECT review_status FROM stored_file WHERE id=?",String.class,f));
  assertEquals("APPROVED",db.queryForObject("SELECT review_status FROM internship_material WHERE file_ref=?",String.class,"/api/portal/files/"+second+"/download"));
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Reassign files"),200);
  send(get("/api/portal/files/"+f),teacher,null,404);
 }
 @Test void resumesAreSharedOnlyByExplicitApplicationAndWithdrawalRevokesAccess()throws Exception{
  byte[] bytes="Demo resume".getBytes();long f=upload(student,"RESUME",null,"resume.txt",bytes,200);
  send(get("/api/portal/files/"+f+"/download"),recruiter,null,404);
  send(post("/api/portal/applications"),other,Map.of("jobId",job,"resumeFileId",f),404);
  long app=send(post("/api/portal/applications"),student,Map.of("jobId",job,"resumeFileId",f),200).path("id").asLong();
  assertEquals(f,send(get("/api/portal/applications/"+app),recruiter,null,200).path("resumeFile").path("id").asLong());
  mvc.perform(as(get("/api/portal/files/"+f+"/download"),recruiter)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
  send(get("/api/portal/files/"+f),otherRecruiter,null,404);
  transition(app,student,"WITHDRAWN");send(get("/api/portal/files/"+f+"/download"),recruiter,null,404);
  assertFalse(send(get("/api/portal/applications/"+app),recruiter,null,200).has("resumeFile"));
  send(get("/api/portal/files/"+f),student,null,200);
 }
 @Test void invalidFileNamesTypesSizesAndBindingsDoNotWriteMetadata()throws Exception{
  byte[] bytes="Not a PDF".getBytes();
  upload(student,"RESUME",null,"../resume.txt",bytes,400);
  upload(student,"RESUME",null,"resume\n\n.txt",bytes,400);
  upload(student,"RESUME",null,"fake.pdf",bytes,400);
  upload(student,"RESUME",null,"script.svg",bytes,400);
  upload(student,"RESUME",null,"empty.txt",new byte[0],400);
  upload(student,"RESUME",null,"large.txt",new byte[10*1024*1024+1],413);
  upload(student,"INSURANCE",null,"insurance.txt",bytes,400);
  upload(recruiter,"RESUME",null,"resume.txt",bytes,403);
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM stored_file WHERE owner_user_id=?",Integer.class,userId(student)));
 }
 @Test void missingOrCorruptFileReturnsUsefulErrorAndNoStoragePath()throws Exception{
  long f=upload(student,"RESUME",null,"resume.txt","Demo".getBytes(),200);
  db.update("UPDATE stored_file SET sha256=REPEAT('0',64) WHERE id=?",f);
  send(get("/api/portal/files/"+f+"/download"),student,null,503);
  db.update("UPDATE stored_file SET storage_key='../outside' WHERE id=?",f);
  send(get("/api/portal/files/"+f+"/download"),student,null,503);
 }

 Map<String,Object> transferBody(){var s=selfBody();return new HashMap<>(Map.of("source","SELF","self",s,"startDate",s.get("startDate"),"endDate",s.get("endDate"),"reason","Verified new arrangement"));}
 long requestTransfer(long p,Map<String,Object> body)throws Exception{return send(post("/api/portal/placements/"+p+"/transfers"),student,body,200).path("id").asLong();}
 JsonNode reviewTransfer(long r,String decision,int status)throws Exception{return send(post("/api/portal/transfers/"+r+"/review"),admin,Map.of("decision",decision,"comment","School verified "+decision),status);}
 @Test void transferReturnResubmitCreatesLinkedRecordAndFreezesOriginalBusiness()throws Exception{
  long p=processPlacement();assign(p);arrive(p);enableAttendance(p);
  long report=send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),200).path("id").asLong();send(post("/api/portal/reports/"+report+"/submit"),student,null,200);
  long leave=send(post("/api/portal/placements/"+p+"/attendance"),student,attendanceBody("LEAVE",1),200).path("id").asLong();
  byte[] bytes="Original agreement".getBytes();long f=upload(student,"AGREEMENT",p,"agreement.txt",bytes,200);
  var body=transferBody();long r=requestTransfer(p,body);String url="/api/portal/transfers/"+r;
  reviewTransfer(r,"RETURNED",200);var updated=new HashMap<>((Map<String,Object>)body.get("self"));updated.put("address","Verified new address");body.put("self",updated);body.put("reason","Supplemented handover arrangement");
  send(post(url+"/resubmit"),student,body,200);long n=reviewTransfer(r,"APPROVED",200).path("replacementPlacementId").asLong();assertNotEquals(p,n);
  var next=send(get("/api/portal/placements/"+n),student,null,200).path("placement");
  assertEquals(p,next.path("previousPlacementId").asLong());assertFalse(next.hasNonNull("teacherId"));assertFalse(next.hasNonNull("enterpriseMentorId"));assertEquals("NOT_ARRIVED",next.path("arrivalStatus").asText());assertEquals("APPROVED",next.path("schoolApprovalStatus").asText());
  var old=send(get("/api/portal/placements/"+p),mentor,null,200).path("placement");assertEquals(n,old.path("replacementPlacementId").asLong());assertEquals(enterprise,old.path("enterpriseId").asLong());assertEquals(userId(teacher),old.path("teacherId").asLong());assertEquals("APPROVED",old.path("schoolApprovalStatus").asText());
  var d=send(get(url),student,null,200);assertEquals(4,d.path("events").size());assertEquals("Demo address",json.readTree(d.path("events").get(0).path("requested_snapshot").asText()).path("self").path("address").asText());assertEquals(n,d.path("replacementPlacementId").asLong());
  for(String who:List.of(teacher,mentor))send(get("/api/portal/placements/"+n),who,null,404);
  String newMentor=account(school,null,"ENTERPRISE_MENTOR");db.update("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(?,?)",userId(newMentor),next.path("enterpriseId").asLong());
  send(post("/api/portal/placements/"+n+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(newMentor),"note","New guidance relationship"),200);
  for(String who:List.of(outsider,newMentor)){send(get("/api/portal/placements/"+n),who,null,200);send(get("/api/portal/files/"+f),who,null,404);}
  for(String who:List.of(teacher,mentor))send(get("/api/portal/placements/"+n),who,null,404);
  send(get("/api/portal/reports/"+report),teacher,null,200);mvc.perform(as(get("/api/portal/files/"+f+"/download"),mentor)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
  assertEquals(0,send(get("/api/portal/files?placementId="+n),student,null,200).path("total").asInt());assertFalse(send(get("/api/portal/placements/"+n+"/attendance"),student,null,200).path("enabled").asBoolean());
  send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),400);send(post("/api/portal/reports/"+report+"/review"),teacher,Map.of("decision","REVIEWED","feedback","Old report"),400);
  send(post("/api/portal/attendance/"+leave+"/review"),teacher,Map.of("decision","APPROVED","feedback","Old leave"),400);send(post("/api/portal/placements/"+p+"/attendance"),student,attendanceBody("MAKE_UP",-1),400);
  upload(student,"AGREEMENT",p,"supplement.txt",bytes,400);send(post("/api/portal/files/"+f+"/review"),teacher,Map.of("decision","APPROVED","comment","Old file"),400);
  send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),400);send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Old guidance"),400);
  send(post("/api/portal/placements/"+p+"/transfers"),student,transferBody(),400);reviewTransfer(r,"APPROVED",400);
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM placement_replacement WHERE previous_placement_id=?",Integer.class,p));
  assertEquals("SUBMITTED",db.queryForObject("SELECT status FROM progress_report WHERE id=?",String.class,report));assertEquals("PENDING",db.queryForObject("SELECT status FROM attendance_record WHERE id=?",String.class,leave));
 }
 @Test void rejectedPlacementCanBeRearrangedAndTransferRejectionAllowsNewRequest()throws Exception{
  long p=processPlacement();db.update("UPDATE internship_placement SET school_approval_status='REJECTED' WHERE id=?",p);accountsApprovalForTest(p);
  long r=requestTransfer(p,transferBody());reviewTransfer(r,"REJECTED",200);assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM placement_replacement WHERE previous_placement_id=?",Integer.class,p));
  send(post("/api/portal/transfers/"+r+"/resubmit"),student,transferBody(),400);
  long second=requestTransfer(p,transferBody());long n=reviewTransfer(second,"APPROVED",200).path("replacementPlacementId").asLong();
  assertEquals("REJECTED",send(get("/api/portal/placements/"+p),student,null,200).path("placement").path("schoolApprovalStatus").asText());
  assertEquals(1,send(get("/api/portal/placements/"+p),student,null,200).path("approvals").size());assertEquals("APPROVED",send(get("/api/portal/placements/"+n),student,null,200).path("placement").path("schoolApprovalStatus").asText());
 }
 void accountsApprovalForTest(long p){db.update("INSERT INTO approval_record(placement_id,approver_id,decision,comment) VALUES(?,?,'REJECTED','Original rejection')",p,userId(admin));}
 @Test void transferPlatformRequiresOwnUnusedAcceptedOfferAndSupportsSameCompanyJobChange()throws Exception{
  long p=processPlacement();long app=apply();var body=new HashMap<String,Object>(Map.of("source","PLATFORM","applicationId",app,"startDate","2027-03-01","endDate","2027-06-30","reason","New platform offer"));
  send(post("/api/portal/placements/"+p+"/transfers"),student,body,400);transition(app,recruiter,"OFFERED");transition(app,student,"ACCEPTED");
  long r=requestTransfer(p,body);assertEquals("CHANGE_JOB",send(get("/api/portal/transfers/"+r),student,null,200).path("request").path("changeType").asText());
  long n=reviewTransfer(r,"APPROVED",200).path("replacementPlacementId").asLong();assertEquals(app,send(get("/api/portal/placements/"+n),student,null,200).path("placement").path("applicationId").asLong());
  send(post("/api/portal/placements/"+n+"/transfers"),student,body,400);
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM internship_placement WHERE application_id=?",Integer.class,app));
  var offers=send(get("/api/portal/applications"),student,null,200).path("items");assertTrue(offers.findValues("placementAssigned").stream().anyMatch(JsonNode::asBoolean));
  assign(n);send(get("/api/portal/placements/"+n),mentor,null,200);
  long second=requestTransfer(n,transferBody());long latest=reviewTransfer(second,"APPROVED",200).path("replacementPlacementId").asLong();assertEquals(n,send(get("/api/portal/placements/"+latest),student,null,200).path("placement").path("previousPlacementId").asLong());
  assertEquals(n,send(get("/api/portal/placements/"+p),student,null,200).path("placement").path("replacementPlacementId").asLong());
 }
 @Test void transferRolesSnapshotsDatesAndConcurrentChangeBoundaries()throws Exception{
  long p=processPlacement();assign(p);String base="/api/portal/placements/"+p+"/transfers";var body=transferBody();
  send(post(base),teacher,body,403);send(post(base),other,body,404);send(post(base),foreignAdmin,body,403);
  var invalid=new HashMap<>(body);invalid.put("startDate","2026-01-01");send(post(base),student,invalid,400);invalid=new HashMap<>(body);invalid.remove("self");send(post(base),student,invalid,400);
  var wrong=new HashMap<>((Map<String,Object>)body.get("self"));wrong.put("contactName"," ");invalid=new HashMap<>(body);invalid.put("self",wrong);send(post(base),student,invalid,400);
  db.update("UPDATE internship_placement SET school_approval_status='PENDING' WHERE id=?",p);send(post(base),student,body,400);db.update("UPDATE internship_placement SET school_approval_status='APPROVED' WHERE id=?",p);
  long r=requestTransfer(p,body);String url="/api/portal/transfers/"+r;
  for(String who:List.of(other,foreignAdmin,outsider))send(get(url),who,null,404);
  send(post(url+"/review"),teacher,Map.of("decision","APPROVED","comment","Not school"),403);send(post(url+"/review"),foreignAdmin,Map.of("decision","APPROVED","comment","Wrong school"),404);
  send(post(base),student,body,400);send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),400);
  db.update("UPDATE internship_placement SET end_date=end_date+INTERVAL 1 DAY WHERE id=?",p);reviewTransfer(r,"APPROVED",400);assertEquals("PENDING",send(get(url),student,null,200).path("request").path("status").asText());reviewTransfer(r,"REJECTED",200);
  long extend=send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),200).path("id").asLong();send(post(base),student,body,400);send(post("/api/portal/extensions/"+extend+"/review"),admin,Map.of("decision","REJECTED","comment","Resolve prior change"),200);
 }
 @Test void transferRevalidatesTargetAndRejectsForeignOffersOrUnchangedArrangement()throws Exception{
  long p=processPlacement();long app=apply();transition(app,recruiter,"OFFERED");transition(app,student,"ACCEPTED");
  var body=new HashMap<String,Object>(Map.of("source","PLATFORM","applicationId",app,"startDate","2027-03-01","endDate","2027-06-30","reason","New platform arrangement"));
  long r=requestTransfer(p,body);db.update("UPDATE enterprise SET review_status='SUSPENDED' WHERE id=?",enterprise);reviewTransfer(r,"APPROVED",400);db.update("UPDATE enterprise SET review_status='APPROVED' WHERE id=?",enterprise);
  db.update("UPDATE job_position SET title='Changed target' WHERE id=?",job);reviewTransfer(r,"APPROVED",400);reviewTransfer(r,"RETURNED",200);send(post("/api/portal/transfers/"+r+"/resubmit"),student,body,200);reviewTransfer(r,"REJECTED",200);
  db.update("UPDATE job_application SET student_id=(SELECT id FROM student_profile WHERE user_id=?) WHERE id=?",userId(other),app);send(post("/api/portal/placements/"+p+"/transfers"),student,body,404);
  var same=transferBody();var details=new HashMap<>((Map<String,Object>)same.get("self"));details.put("enterpriseName","IT Enterprise");details.put("creditCode",db.queryForObject("SELECT UPPER(credit_code) FROM enterprise WHERE id=?",String.class,enterprise));details.put("positionTitle","Process Test");same.put("self",details);send(post("/api/portal/placements/"+p+"/transfers"),student,same,400);
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM placement_replacement WHERE previous_placement_id=?",Integer.class,p));
 }

 long termination(long p)throws Exception{return send(post("/api/portal/placements/"+p+"/terminations"),student,Map.of("reason","Original termination handover"),200).path("id").asLong();}
 void terminateReview(long r,String decision,int status)throws Exception{send(post("/api/portal/terminations/"+r+"/review"),admin,Map.of("decision",decision,"comment","School termination "+decision),status);}
 @Test void terminationRoundTripFreezesBusinessAndPreservesAllHistory()throws Exception{
  long p=processPlacement();assign(p);arrive(p);enableAttendance(p);String end=placementEnd(p);
  long report=send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),200).path("id").asLong();send(post("/api/portal/reports/"+report+"/submit"),student,null,200);
  long attendance=send(post("/api/portal/placements/"+p+"/attendance"),student,attendanceBody("LEAVE",1),200).path("id").asLong();
  byte[] bytes="Historical termination material".getBytes();long f=upload(student,"AGREEMENT",p,"history.txt",bytes,200);
  long r=termination(p);String url="/api/portal/terminations/"+r;terminateReview(r,"RETURNED",200);
  send(post(url+"/resubmit"),student,Map.of("reason","Supplemented termination handover"),200);terminateReview(r,"APPROVED",200);
  var d=send(get(url),mentor,null,200);assertEquals(4,d.path("events").size());assertEquals("Original termination handover",d.path("events").get(0).path("reason").asText());assertEquals("School termination RETURNED",d.path("events").get(1).path("review_comment").asText());
  var placement=send(get("/api/portal/placements/"+p),student,null,200).path("placement");assertEquals(r,placement.path("terminationRequestId").asLong());assertTrue(placement.hasNonNull("terminatedAt"));assertEquals("APPROVED",placement.path("schoolApprovalStatus").asText());assertEquals(end,placement.path("endDate").asText());assertEquals(userId(teacher),placement.path("teacherId").asLong());
  for(String who:List.of(student,admin,teacher,mentor))send(get("/api/portal/placements/"+p+"/terminations"),who,null,200);
  send(post("/api/portal/placements/"+p+"/reports"),student,reportBody(),400);send(post("/api/portal/reports/"+report+"/review"),teacher,Map.of("decision","REVIEWED","feedback","Blocked"),400);
  send(post("/api/portal/placements/"+p+"/attendance"),student,attendanceBody("CHECK_IN",0),400);send(post("/api/portal/attendance/"+attendance+"/review"),teacher,Map.of("decision","APPROVED","feedback","Blocked"),400);
  send(put("/api/portal/placements/"+p+"/attendance-policy"),admin,Map.of("enabled",false,"note","Blocked"),400);
  send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Blocked"),400);
  send(post("/api/portal/placements/"+p+"/arrival"),mentor,Map.of("arrivalDate",java.time.LocalDate.now().toString(),"note","Blocked"),400);
  send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),400);send(post("/api/portal/placements/"+p+"/terminations"),student,Map.of("reason","Again"),400);
  upload(student,"AGREEMENT",p,"new.txt",bytes,400);send(post("/api/portal/files/"+f+"/review"),admin,Map.of("decision","APPROVED","comment","Blocked"),400);
  mvc.perform(as(get("/api/portal/files/"+f+"/download"),mentor)).andExpect(status().isOk()).andExpect(content().bytes(bytes));send(get("/api/portal/reports/"+report),teacher,null,200);send(get("/api/portal/attendance/"+attendance),teacher,null,200);
  assertEquals("SUBMITTED",db.queryForObject("SELECT status FROM progress_report WHERE id=?",String.class,report));assertEquals("PENDING",db.queryForObject("SELECT status FROM attendance_record WHERE id=?",String.class,attendance));
  terminateReview(r,"APPROVED",400);send(post(url+"/resubmit"),student,Map.of("reason","Again"),400);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM placement_termination WHERE placement_id=?",Integer.class,p));
 }
 @Test void terminationScopeValidationAndMutualExclusion()throws Exception{
  long p=processPlacement();assign(p);String base="/api/portal/placements/"+p+"/terminations";var body=Map.of("reason","Termination arrangement");
  send(post(base),student,Map.of("reason"," "),400);send(post(base),student,Map.of("reason","x".repeat(481)),400);
  for(String who:List.of(admin,teacher,mentor))send(post(base),who,body,403);send(post(base),other,body,404);
  db.update("UPDATE internship_placement SET school_approval_status='REJECTED' WHERE id=?",p);send(post(base),student,body,400);db.update("UPDATE internship_placement SET school_approval_status='APPROVED' WHERE id=?",p);
  long r=termination(p);String url="/api/portal/terminations/"+r;
  for(String who:List.of(other,outsider,foreignAdmin))send(get(url),who,null,404);
  send(get(url),recruiter,null,403);send(post(url+"/review"),student,Map.of("decision","APPROVED","comment","Blocked"),403);send(post(url+"/review"),foreignAdmin,Map.of("decision","APPROVED","comment","Blocked"),404);
  send(post(base),student,body,400);send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),400);send(post("/api/portal/placements/"+p+"/transfers"),student,transferBody(),400);
  terminateReview(r,"RETURNED",200);send(post(base),student,body,400);send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Returned cannot approve"),400);
  send(post(url+"/resubmit"),other,body,404);send(post(url+"/resubmit"),student,Map.of("reason","Updated"),200);terminateReview(r,"REJECTED",200);
  assertFalse(send(get("/api/portal/placements/"+p),student,null,200).path("placement").hasNonNull("terminationRequestId"));
  long ext=send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(90),200).path("id").asLong();send(post(base),student,body,400);send(post("/api/portal/extensions/"+ext+"/review"),admin,Map.of("decision","REJECTED","comment","Resolve extension"),200);
  long transfer=requestTransfer(p,transferBody());send(post(base),student,body,400);reviewTransfer(transfer,"REJECTED",200);termination(p);
 }
 @Test void staleTerminationCannotFreezeChangedPlacementAndRejectedRequestCanBeReplaced()throws Exception{
  long p=processPlacement();long r=termination(p);db.update("UPDATE internship_placement SET end_date=end_date+INTERVAL 1 DAY WHERE id=?",p);
  terminateReview(r,"APPROVED",400);assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM placement_termination WHERE placement_id=?",Integer.class,p));
  terminateReview(r,"RETURNED",200);send(post("/api/portal/terminations/"+r+"/resubmit"),student,Map.of("reason","Stale"),400);
  terminateReview(r,"REJECTED",200);
  long next=termination(p);terminateReview(next,"APPROVED",200);assertEquals(next,send(get("/api/portal/placements/"+p),student,null,200).path("placement").path("terminationRequestId").asLong());
 }
 @Test void terminatedPlacementResumesThroughLinkedNewRecordIncludingSameSelfArrangement()throws Exception{
  var self=selfBody();long p=send(post("/api/portal/self-placements"),student,self,200).path("id").asLong();send(post("/api/portal/self-placements/"+p+"/submit"),student,null,200);send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Verified"),200);
  long r=termination(p);terminateReview(r,"APPROVED",200);
  send(post("/api/portal/self-placements"),student,self,400);send(put("/api/portal/self-placements/"+p),student,self,400);send(post("/api/portal/self-placements/"+p+"/submit"),student,null,400);
  var body=new HashMap<String,Object>(Map.of("source","SELF","self",self,"startDate",self.get("startDate"),"endDate",self.get("endDate"),"reason","Continue through a separate record"));long transfer=requestTransfer(p,body);
  assertEquals(r,json.readTree(send(get("/api/portal/transfers/"+transfer),student,null,200).path("request").path("originalSnapshot").asText()).path("terminationRequestId").asLong());
  long n=reviewTransfer(transfer,"APPROVED",200).path("replacementPlacementId").asLong();var next=send(get("/api/portal/placements/"+n),student,null,200).path("placement");assertFalse(next.hasNonNull("terminationRequestId"));assertFalse(next.hasNonNull("teacherId"));assertEquals("NOT_ARRIVED",next.path("arrivalStatus").asText());assertEquals(p,next.path("previousPlacementId").asLong());
  assertEquals(r,send(get("/api/portal/placements/"+p),student,null,200).path("placement").path("terminationRequestId").asLong());send(post("/api/portal/placements/"+p+"/transfers"),student,body,400);termination(n);
 }

 @Test void helpCaseAssignmentConfirmationAndSchoolReviewKeepSnapshots()throws Exception{
  long p=processPlacement();long c=send(post("/api/portal/cases"),student,Map.of("title","Need assistance","description","Original assistance request"),200).path("id").asLong();String url="/api/portal/cases/"+c;
  send(get(url),teacher,null,404);send(get(url),other,null,404);send(get(url),foreignAdmin,null,404);send(get(url),mentor,null,403);
  send(post(url+"/assign"),admin,Map.of("ownerId",userId(teacher),"note","Assign case teacher"),200);
  send(post(url+"/follow"),teacher,Map.of("note","Contacted student"),200);send(post(url+"/resolve"),teacher,Map.of("note","First result"),200);
  send(post(url+"/review"),admin,Map.of("decision","CLOSE","note","Too early"),400);
  send(post(url+"/confirm"),student,Map.of("decision","REOPEN","note","More help needed"),200);send(post(url+"/resolve"),teacher,Map.of("note","Complete result"),200);send(post(url+"/confirm"),student,Map.of("decision","ACCEPT","note","Confirmed"),200);
  send(post(url+"/review"),admin,Map.of("decision","RETURN","note","Verify evidence"),200);send(post(url+"/resolve"),teacher,Map.of("note","Verified complete result"),200);send(post(url+"/confirm"),student,Map.of("decision","ACCEPT","note","Verified"),200);send(post(url+"/review"),admin,Map.of("decision","CLOSE","note","School verified"),200);
  var d=send(get(url),student,null,200);assertEquals("CLOSED",d.path("case").path("status").asText());assertEquals(11,d.path("events").size());assertEquals("Original assistance request",d.path("events").get(0).path("note").asText());
  send(post(url+"/follow"),student,Map.of("note","Closed"),400);send(post(url+"/resolve"),teacher,Map.of("note","Closed"),400);
 }
 @Test void helpCaseReassignmentAndRoleBoundariesRemainPrivate()throws Exception{
  processPlacement();long c=send(post("/api/portal/cases"),student,Map.of("title","Private case","description","Private description"),200).path("id").asLong();String url="/api/portal/cases/"+c;
  send(post(url+"/assign"),student,Map.of("ownerId",userId(teacher),"note","Unauthorized"),403);send(post(url+"/assign"),admin,Map.of("ownerId",userId(foreignAdmin),"note","Wrong school"),400);
  send(post(url+"/assign"),admin,Map.of("ownerId",userId(teacher),"note","First owner"),200);send(post(url+"/assign"),admin,Map.of("ownerId",userId(outsider),"note","Replacement owner"),200);
  send(get(url),teacher,null,404);send(get(url),outsider,null,200);send(post(url+"/resolve"),teacher,Map.of("note","Former owner"),404);send(post(url+"/resolve"),admin,Map.of("note","Not owner"),400);
  send(post(url+"/follow"),other,Map.of("note","Other student"),404);send(post("/api/portal/cases"),student,Map.of("title"," ","description","Invalid"),400);
 }

 Map<String,Object> warningPolicy(){return new HashMap<>(Map.of("unplaced",true,"unplacedGraceDays",0,"materials",true,"materialDays",0,"requiredKinds",List.of("AGREEMENT"),"reports",true,"frequencyDays",7,"reportGraceDays",0,"contact",true,"contactDays",7));}
 @Test void warningRulesGenerateEvidenceDeduplicateAndRecoverEpisodes()throws Exception{
  long p=processPlacement();assign(p);arrive(p);db.update("UPDATE internship_batch SET start_date=CURRENT_DATE-INTERVAL 30 DAY,end_date=CURRENT_DATE+INTERVAL 60 DAY WHERE id=?",batch);String url="/api/portal/batches/"+batch+"/warnings";
  send(post(url+"/scan"),admin,null,400);send(put(url),admin,warningPolicy(),200);
  var futureReport=new HashMap<>(reportBody());futureReport.put("periodStart",java.time.LocalDate.now().minusDays(7).toString());futureReport.put("periodEnd",java.time.LocalDate.now().plusDays(60).toString());long future=send(post("/api/portal/placements/"+p+"/reports"),student,futureReport,200).path("id").asLong();send(post("/api/portal/reports/"+future+"/submit"),student,null,200);
  var scan=send(post(url+"/scan"),admin,null,200);assertEquals(4,scan.path("createdCaseIds").size());assertEquals(0,send(post(url+"/scan"),admin,null,200).path("createdCaseIds").size());
  send(post("/api/portal/placements/"+p+"/contacts"),teacher,Map.of("note","Actual guidance contact"),200);send(post(url+"/scan"),admin,null,200);assertEquals(0,db.queryForObject("SELECT active FROM warning_detection WHERE detection_key=?",Integer.class,batch+":"+studentId+":CONTACT"));
  db.update("UPDATE guidance_record SET contact_at=CURRENT_DATE-INTERVAL 8 DAY WHERE placement_id=?",p);assertEquals(1,send(post(url+"/scan"),admin,null,200).path("createdCaseIds").size());
  assertTrue(db.queryForObject("SELECT COUNT(*) FROM student_case WHERE student_id=? AND kind='WARNING' AND evidence IS NOT NULL",Integer.class,studentId)>=4);
  var disabled=warningPolicy();for(String k:List.of("unplaced","materials","reports","contact"))disabled.put(k,false);send(put(url),admin,disabled,200);assertEquals(0,send(post(url+"/scan"),admin,null,200).path("createdCaseIds").size());assertEquals(2,send(get(url),admin,null,200).path("events").size());
 }
 @Test void warningPolicyAndGuidanceContactEnforceSchoolRoleAndTermination()throws Exception{
  long p=processPlacement();assign(p);String url="/api/portal/batches/"+batch+"/warnings";send(put(url),student,warningPolicy(),403);send(put(url),foreignAdmin,warningPolicy(),404);
  var invalid=warningPolicy();invalid.put("frequencyDays",0);send(put(url),admin,invalid,400);invalid=warningPolicy();invalid.put("requiredKinds",List.of("AGREEMENT","AGREEMENT"));send(put(url),admin,invalid,400);
  send(post("/api/portal/placements/"+p+"/contacts"),student,Map.of("note","No teacher"),403);send(post("/api/portal/placements/"+p+"/contacts"),outsider,Map.of("note","Not assigned"),404);
  long r=termination(p);terminateReview(r,"APPROVED",200);send(post("/api/portal/placements/"+p+"/contacts"),teacher,Map.of("note","Terminated"),400);send(get("/api/portal/placements/"+p+"/contacts"),mentor,null,200);
 }

 Map<String,Object> gradePolicy(){return Map.of("studentWeight",20,"enterpriseWeight",30,"teacherWeight",50,"passScore",60,"note","School batch assessment policy");}
 void evaluate(long p,String who,int score)throws Exception{send(put("/api/portal/placements/"+p+"/evaluations"),who,Map.of("score",score,"comment","Verified evaluation by "+role(who)),200);send(post("/api/portal/placements/"+p+"/evaluations/submit"),who,null,200);}
 @Test void threePartyEvaluationUsesConfiguredWeightsAndImmutableSnapshots()throws Exception{
  long p=processPlacement();assign(p);arrive(p);String url="/api/portal/placements/"+p+"/evaluations";
  assertFalse(send(get(url),student,null,200).has("calculation"));send(put(url),student,Map.of("score",80,"comment","Self draft"),200);assertEquals(0,send(get(url),teacher,null,200).path("evaluations").size());send(post(url+"/submit"),student,null,200);evaluate(p,mentor,90);evaluate(p,teacher,85);assertFalse(send(get(url),student,null,200).has("calculation"));
  send(put("/api/portal/batches/"+batch+"/grading"),admin,gradePolicy(),200);var calc=send(get(url),student,null,200).path("calculation");assertEquals(85.5,calc.path("total").asDouble());assertTrue(calc.path("passed").asBoolean());assertEquals(1,calc.path("policyVersion").asInt());
  send(put(url),teacher,Map.of("score",99,"comment","Submitted cannot edit"),400);assertEquals(2,send(get(url+"/TEACHER"),student,null,200).path("events").size());
  send(put("/api/portal/batches/"+batch+"/grading"),admin,Map.of("studentWeight",10,"enterpriseWeight",40,"teacherWeight",50,"passScore",90,"note","Updated policy"),200);calc=send(get(url),student,null,200).path("calculation");assertEquals(86.5,calc.path("total").asDouble());assertFalse(calc.path("passed").asBoolean());assertEquals(2,calc.path("policyVersion").asInt());assertEquals(2,send(get("/api/portal/batches/"+batch+"/grading"),admin,null,200).path("events").size());
 }
 @Test void evaluationSchoolRoleScoreAndCurrentMentorBoundaries()throws Exception{
  long p=processPlacement();assign(p);arrive(p);String url="/api/portal/placements/"+p+"/evaluations";
  send(put("/api/portal/batches/"+batch+"/grading"),foreignAdmin,gradePolicy(),404);send(put("/api/portal/batches/"+batch+"/grading"),student,gradePolicy(),403);send(put("/api/portal/batches/"+batch+"/grading"),admin,Map.of("studentWeight",20,"enterpriseWeight",20,"teacherWeight",20,"passScore",60,"note","Bad weights"),400);
  for(int score:List.of(-1,101))send(put(url),student,Map.of("score",score,"comment","Invalid"),400);send(put(url),admin,Map.of("score",80,"comment","Admin cannot impersonate"),403);send(put(url),other,Map.of("score",80,"comment","Foreign student"),404);
  evaluate(p,teacher,85);send(post("/api/portal/placements/"+p+"/mentors"),admin,Map.of("teacherId",userId(outsider),"enterpriseMentorId",userId(mentor),"note","Reassignment invalidates former evaluation"),200);send(get(url),teacher,null,404);send(put(url),outsider,Map.of("score",90,"comment","Current teacher draft"),200);
  long term=termination(p);terminateReview(term,"APPROVED",200);send(post(url+"/submit"),outsider,null,400);send(get(url+"/TEACHER"),admin,null,200);
 }

 long gradedPlacement()throws Exception{long p=processPlacement();assign(p);arrive(p);send(put("/api/portal/batches/"+batch+"/grading"),admin,gradePolicy(),200);evaluate(p,student,80);evaluate(p,mentor,90);evaluate(p,teacher,85);return p;}
 long gradeRequest(long p)throws Exception{return send(post("/api/portal/placements/"+p+"/grade-reviews"),student,Map.of("reason","Please verify assessment"),200).path("id").asLong();}
 void reviewGrade(long r,String decision,List<String> types,int expected)throws Exception{send(post("/api/portal/grade-reviews/"+r+"/review"),admin,Map.of("decision",decision,"comment","School grade "+decision,"returnTypes",types),expected);}
 @Test void gradeReviewReturnSupplementApprovalKeepsBothScoreSnapshots()throws Exception{
  long p=gradedPlacement(),r=gradeRequest(p);String url="/api/portal/grade-reviews/"+r;
  send(post("/api/portal/placements/"+p+"/grade-reviews"),student,Map.of("reason","Duplicate"),400);reviewGrade(r,"RETURNED",List.of("TEACHER"),200);assertEquals("RETURNED",send(get("/api/portal/placements/"+p+"/evaluations/TEACHER"),teacher,null,200).path("evaluation").path("status").asText());send(post(url+"/resubmit"),student,Map.of("reason","Not supplemented"),400);
  evaluate(p,teacher,90);send(post(url+"/resubmit"),student,Map.of("reason","Teacher supplemented evidence"),200);reviewGrade(r,"APPROVED",List.of(),200);
  var d=send(get(url),student,null,200);assertEquals(4,d.path("events").size());assertEquals(85.5,json.readTree(d.path("events").get(0).path("snapshot").asText()).path("total").asDouble());assertEquals(88.0,json.readTree(d.path("events").get(3).path("snapshot").asText()).path("total").asDouble());assertEquals(r,send(get("/api/portal/placements/"+p+"/grade-reviews"),student,null,200).path("approvedRequestId").asLong());
  long appeal=gradeRequest(p);assertFalse(send(get("/api/portal/placements/"+p+"/grade-reviews"),student,null,200).has("approvedRequestId"));reviewGrade(appeal,"REJECTED",List.of(),200);assertEquals(r,send(get("/api/portal/placements/"+p+"/grade-reviews"),student,null,200).path("approvedRequestId").asLong());reviewGrade(r,"APPROVED",List.of(),400);
 }
 @Test void gradeReviewRejectsStalePolicyForeignActorsAndInvalidReturns()throws Exception{
  long p=gradedPlacement(),r=gradeRequest(p);String url="/api/portal/grade-reviews/"+r;
  send(post(url+"/review"),teacher,Map.of("decision","APPROVED","comment","Not school","returnTypes",List.of()),403);send(post(url+"/review"),foreignAdmin,Map.of("decision","APPROVED","comment","Foreign school","returnTypes",List.of()),404);send(get(url),other,null,404);
  reviewGrade(r,"RETURNED",List.of(),400);reviewGrade(r,"RETURNED",List.of("TEACHER","TEACHER"),400);reviewGrade(r,"APPROVED",List.of("TEACHER"),400);
  var changed=new HashMap<>(gradePolicy());changed.put("note","New policy version");send(put("/api/portal/batches/"+batch+"/grading"),admin,changed,200);reviewGrade(r,"APPROVED",List.of(),400);reviewGrade(r,"REJECTED",List.of(),200);
  long next=gradeRequest(p);reviewGrade(next,"APPROVED",List.of(),200);long term=termination(p);terminateReview(term,"APPROVED",200);send(post("/api/portal/placements/"+p+"/grade-reviews"),student,Map.of("reason","Terminated"),400);send(get(url),mentor,null,200);
 }

 long resultFile(long p)throws Exception{long f=upload(student,"RESULT",p,"result.txt","Verified synthetic internship result".getBytes(),200);send(post("/api/portal/files/"+f+"/review"),teacher,Map.of("decision","APPROVED","comment","Result checked"),200);return f;}
 Map<String,Object> archiveBody(long f){return Map.of("summary","Verified internship summary","resultFileId",f);}
 long[] archiveReady()throws Exception{long p=gradedPlacement();db.update("UPDATE internship_placement SET end_date=CURRENT_DATE-INTERVAL 1 DAY WHERE id=?",p);long r=gradeRequest(p);reviewGrade(r,"APPROVED",List.of(),200);return new long[]{p,resultFile(p)};}
 @Test void archiveReturnResubmitFreezesSnapshotAndZipPreservesOriginalBytes()throws Exception{
  var fixture=archiveReady();long p=fixture[0],f=fixture[1];String url="/api/portal/placements/"+p+"/archive";send(post(url),student,archiveBody(f),200);send(post(url+"/review"),admin,Map.of("decision","RETURNED","comment","Add summary evidence"),200);send(post(url+"/resubmit"),student,Map.of("summary","Supplemented summary evidence","resultFileId",f),200);send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Complete archive approved"),200);
  var d=send(get(url),student,null,200);assertEquals(4,d.path("events").size());assertEquals("APPROVED",d.path("placement").path("archiveStatus").asText());assertTrue(d.path("archive").hasNonNull("archivedAt"));String snapshot=d.path("archive").path("snapshot").asText();assertFalse(json.readTree(snapshot).path("files").get(0).has("storageKey"));
  send(put("/api/portal/batches/"+batch+"/grading"),admin,Map.of("studentWeight",100,"enterpriseWeight",0,"teacherWeight",0,"passScore",99,"note","New policy must not rewrite archive"),200);assertEquals(85.5,send(get("/api/portal/placements/"+p+"/evaluations"),student,null,200).path("calculation").path("total").asDouble());assertTrue(send(get("/api/portal/placements/"+p+"/grade-reviews"),student,null,200).has("approvedRequestId"));
  send(put("/api/portal/placements/"+p+"/evaluations"),student,Map.of("score",99,"comment","Frozen"),400);send(post("/api/portal/placements/"+p+"/terminations"),student,Map.of("reason","Frozen"),400);send(post("/api/portal/placements/"+p+"/transfers"),student,transferBody(),400);upload(student,"RESULT",p,"new-result.txt","New".getBytes(),400);
  var response=mvc.perform(as(get(url+"/export"),student)).andExpect(status().isOk()).andExpect(content().contentType("application/zip")).andExpect(header().string("Cache-Control","no-store, private")).andReturn().getResponse().getContentAsByteArray();var entries=new HashMap<String,byte[]>();try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(response),java.nio.charset.StandardCharsets.UTF_8)){java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null)entries.put(entry.getName(),zip.readAllBytes());}assertEquals(snapshot,new String(entries.get("archive.json"),java.nio.charset.StandardCharsets.UTF_8));assertArrayEquals("Verified synthetic internship result".getBytes(),entries.get("files/"+f+"-result.txt"));assertTrue(entries.containsKey("approval.json"));
  send(get(url+"/export"),teacher,null,403);send(get(url+"/export"),other,null,404);send(get(url+"/export"),foreignAdmin,null,404);send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Repeated"),400);
 }
 @Test void archiveStaleMaterialEvidenceRequiresReturnAndResubmission()throws Exception{
  var fixture=archiveReady();long p=fixture[0],f=fixture[1];String url="/api/portal/placements/"+p+"/archive";send(post(url),student,archiveBody(f),200);send(post("/api/portal/files/"+f+"/review"),teacher,Map.of("decision","APPROVED","comment","Clarified approval evidence"),200);send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Stale"),400);assertEquals("PENDING",send(get(url),student,null,200).path("archive").path("status").asText());send(post(url+"/resubmit"),student,archiveBody(f),400);send(post(url+"/review"),admin,Map.of("decision","RETURNED","comment","Confirm new evidence"),200);send(post(url+"/resubmit"),student,archiveBody(f),200);send(post(url+"/review"),admin,Map.of("decision","APPROVED","comment","Current evidence"),200);
 }
 @Test void archiveRequiresEndedReviewedGradeOwnResultAndResolvedWarnings()throws Exception{
  long p=gradedPlacement(),f=resultFile(p);String url="/api/portal/placements/"+p+"/archive";send(post(url),student,archiveBody(f),400);db.update("UPDATE internship_placement SET end_date=CURRENT_DATE-INTERVAL 1 DAY WHERE id=?",p);send(post(url),student,archiveBody(f),400);long r=gradeRequest(p);reviewGrade(r,"APPROVED",List.of(),200);
  long foreignFile=upload(other,"RESUME",null,"other.txt","Other private file".getBytes(),200);send(post(url),student,archiveBody(foreignFile),400);
  long warning=insert("INSERT INTO student_case(school_id,student_id,placement_id,kind,title,description) VALUES(?,?,?,'WARNING','Archive warning','Unresolved')",school,studentId,p);send(post(url),student,archiveBody(f),400);db.update("UPDATE student_case SET status='CLOSED' WHERE id=?",warning);
  long ext=send(post("/api/portal/placements/"+p+"/extensions"),student,extensionBody(80),200).path("id").asLong();send(post(url),student,archiveBody(f),400);send(post("/api/portal/extensions/"+ext+"/review"),admin,Map.of("decision","REJECTED","comment","No extension"),200);send(post(url),student,archiveBody(f),200);send(get(url+"/export"),student,null,400);
 }
 @Test void archiveEnforcesMissingConfigurationAndConfiguredReportCoverage()throws Exception{
  long p=processPlacement();assign(p);arrive(p);db.update("UPDATE internship_placement SET end_date=CURRENT_DATE-INTERVAL 1 DAY WHERE id=?",p);evaluate(p,student,80);evaluate(p,mentor,90);evaluate(p,teacher,85);long f=resultFile(p);String url="/api/portal/placements/"+p+"/archive";send(post(url),student,archiveBody(f),400);
  send(put("/api/portal/batches/"+batch+"/grading"),admin,gradePolicy(),200);long review=gradeRequest(p);reviewGrade(review,"APPROVED",List.of(),200);var policy=warningPolicy();policy.put("unplaced",false);policy.put("materials",false);policy.put("contact",false);send(put("/api/portal/batches/"+batch+"/warnings"),admin,policy,200);send(post(url),student,archiveBody(f),400);
  var body=new HashMap<>(reportBody());body.put("periodStart",java.time.LocalDate.now().minusDays(7).toString());body.put("periodEnd",java.time.LocalDate.now().minusDays(1).toString());long report=send(post("/api/portal/placements/"+p+"/reports"),student,body,200).path("id").asLong();send(post("/api/portal/reports/"+report+"/submit"),student,null,200);send(post(url),student,archiveBody(f),400);send(post("/api/portal/reports/"+report+"/review"),teacher,Map.of("decision","REVIEWED","feedback","Coverage verified"),200);send(post(url),student,archiveBody(f),200);
 }

 @Test void recognizedFileFormatsPreserveMimeAndRequireCsrf()throws Exception{
  byte[] pdf="%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  long p=upload(student,"RESUME",null,"resume.pdf",pdf,200);
  mvc.perform(as(get("/api/portal/files/"+p+"/preview"),student)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
  byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
  long image=upload(student,"RESUME",null,"resume.png",png,200);
  mvc.perform(as(get("/api/portal/files/"+image+"/preview"),student)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  var out=new java.io.ByteArrayOutputStream();try(var zip=new java.util.zip.ZipOutputStream(out)){for(String name:List.of("[Content_Types].xml","word/document.xml")){zip.putNextEntry(new java.util.zip.ZipEntry(name));zip.write("<document/>".getBytes());zip.closeEntry();}}
  byte[] doc=out.toByteArray();long word=upload(student,"RESUME",null,"resume.docx",doc,200);
  send(get("/api/portal/files/"+word+"/preview"),student,null,400);
  mvc.perform(as(get("/api/portal/files/"+word+"/download"),student)).andExpect(status().isOk()).andExpect(content().bytes(doc));
  mvc.perform(multipart("/api/portal/files").file(new org.springframework.mock.web.MockMultipartFile("file","resume.txt","text/plain","Demo".getBytes())).param("kind","RESUME").with(user(student).roles("STUDENT"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/portal/files/"+p+"/download")).andExpect(status().isUnauthorized());
 }
}
