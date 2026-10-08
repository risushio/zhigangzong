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
abstract class PortalTestFixture {
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
}
