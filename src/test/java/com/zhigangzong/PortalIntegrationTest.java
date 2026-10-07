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
