package com.zhigangzong;

import com.fasterxml.jackson.databind.*;
import com.zhigangzong.dto.CreateStandaloneStudentProfileRequest;
import com.zhigangzong.mapper.ManagementMapper;
import com.zhigangzong.service.StudentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real commits/rollbacks in an isolated MySQL database, using only ordinary draft REST calls. */
@SpringBootTest(properties={"app.bootstrap.enabled=false","app.agent.enabled=true","app.agent.model-enabled=false","app.agent.cleanup-enabled=false","app.reminders.enabled=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS", matches="true")
class StudentDraftBusinessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @MockitoSpyBean StudentService students;
    @MockitoSpyBean ManagementMapper audit;
    final Map<Long,String> scopes=new LinkedHashMap<>();
    long school,actor; String username;

    long insert(String sql,Object... args) { db.update(sql,args); return db.queryForObject("SELECT LAST_INSERT_ID()",Long.class); }
    long school() {
        String marker="draft-it-"+UUID.randomUUID().toString().substring(0,16);
        long id=insert("INSERT INTO school(name,code) VALUES('Draft API isolated fixture',?)",marker);
        scopes.put(id,marker);return id;
    }
    String account(long school,String role) {
        String name="draft-it-"+UUID.randomUUID().toString().substring(0,16);
        long id=insert("INSERT INTO user_account(school_id,display_name,role) VALUES(?,'Draft fixture',?)",school,role);
        db.update("INSERT INTO auth_account(user_id,username,password_hash) VALUES(?,?,'test-only')",id,name);return name;
    }
    @BeforeEach void setup() {
        assertTrue(db.queryForObject("SELECT DATABASE()",String.class).matches("zhigangzong_agent_(phase1_test_|test_|ui_test_)[a-z0-9_]+"),"Draft write tests require an isolated Agent test database");
        school=school();username=account(school,"SCHOOL_ADMIN");
        actor=db.queryForObject("SELECT user_id FROM auth_account WHERE username=?",Long.class,username);
    }
    @AfterEach void cleanup() {
        reset(students,audit);
        for(var scope:scopes.entrySet()) {
            long id=scope.getKey();assertEquals(scope.getValue(),db.queryForObject("SELECT code FROM school WHERE id=?",String.class,id));
            db.update("DELETE c FROM agent_tool_call c JOIN agent_message m ON m.id=c.message_id JOIN agent_session s ON s.id=m.session_id WHERE s.school_id=?",id);
            db.update("DELETE FROM agent_action_request WHERE school_id=?",id);
            db.update("DELETE FROM agent_student_draft WHERE school_id=?",id);
            db.update("DELETE m FROM agent_message m JOIN agent_session s ON s.id=m.session_id WHERE s.school_id=?",id);
            db.update("DELETE FROM agent_session WHERE school_id=?",id);
            db.update("DELETE FROM audit_log WHERE actor_id IN (SELECT id FROM user_account WHERE school_id=?)",id);
            db.update("DELETE FROM student_profile WHERE school_id=?",id);
            db.update("DELETE FROM auth_account WHERE user_id IN (SELECT id FROM user_account WHERE school_id=?)",id);
            db.update("DELETE FROM user_account WHERE school_id=?",id);
            db.update("DELETE FROM department WHERE school_id=?",id);
            db.update("DELETE FROM school WHERE id=?",id);
        }
        scopes.clear();
    }
    JsonNode as(MockHttpServletRequestBuilder request,String name,String role,Object body,int expected) throws Exception {
        request.with(user(name).roles(role)).with(csrf());
        // Request JSON must keep explicit nulls; the application's NON_NULL response mapper must not erase CLEAR operations.
        if(body!=null) request.contentType("application/json").content(new ObjectMapper().writeValueAsString(body));
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }
    JsonNode send(MockHttpServletRequestBuilder request,Object body,int expected) throws Exception {return as(request,username,"SCHOOL_ADMIN",body,expected).path("data");}
    String session() throws Exception {return send(post("/api/agent/sessions"),Map.of(),200).path("sessionId").asText();}
    JsonNode draft(Map<String,Object> fields) throws Exception {return send(post("/api/agent/sessions/"+session()+"/drafts"),Map.of("fields",fields),200);}
    String id(JsonNode draft) {return draft.path("draftId").asText();}
    JsonNode updateDraft(JsonNode draft,Map<String,Object> fields) throws Exception {return send(patch("/api/agent/drafts/"+id(draft)),Map.of("expectedVersion",draft.path("version").asInt(),"fields",fields),200);}
    JsonNode confirm(JsonNode draft,String key,int expected) throws Exception {return send(post("/api/agent/drafts/"+id(draft)+"/confirm").header("Idempotency-Key",key),Map.of("expectedVersion",draft.path("version").asInt()),expected);}
    int studentCount() {return db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school);}
    int actionCount() {return db.queryForObject("SELECT COUNT(*) FROM agent_action_request WHERE school_id=?",Integer.class,school);}

    @Test void manualDraftPersistsAndConfirmationUsesStudentServiceWithoutCreatingAccounts() throws Exception {
        int users=db.queryForObject("SELECT COUNT(*) FROM user_account",Integer.class),accounts=db.queryForObject("SELECT COUNT(*) FROM auth_account",Integer.class);
        var d=draft(Map.of("name","虚构学生","studentNo","00060001","gradeYear",2026,"className","软件工程1班"));
        assertEquals("COLLECTING",d.path("status").asText());assertEquals("COLLECTING",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(d)));
        assertEquals(2026,json.readTree(db.queryForObject("SELECT fields_json FROM agent_student_draft WHERE id=?",String.class,id(d))).path("gradeYear").asInt());
        assertEquals(0,studentCount());confirm(d,"missing_major_1",409);
        d=updateDraft(d,Map.of("major","软件工程"));assertEquals("READY",d.path("status").asText());
        var clearing=new LinkedHashMap<String,Object>();clearing.put("className",null);d=updateDraft(d,clearing);
        assertFalse(d.path("fields").hasNonNull("className"));assertTrue(json.readTree(db.queryForObject("SELECT fields_json FROM agent_student_draft WHERE id=?",String.class,id(d))).path("className").isNull());
        assertEquals(2026,d.path("fields").path("gradeYear").asInt());assertEquals("00060001",d.path("fields").path("studentNo").asText());
        d=updateDraft(d,Map.of("className","软件工程2班"));
        var created=confirm(d,"manual_confirm_1",200);long student=created.path("studentId").asLong();
        assertEquals("CREATED",created.path("status").asText());assertEquals("CREATED",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(d)));
        assertEquals(1,studentCount());assertNull(db.queryForObject("SELECT user_id FROM student_profile WHERE id=?",Long.class,student));
        assertEquals(2026,db.queryForObject("SELECT grade_year FROM student_profile WHERE id=?",Integer.class,student));
        assertEquals("软件工程2班",db.queryForObject("SELECT class_name FROM student_profile WHERE id=?",String.class,student));
        var profile=send(get("/api/students/"+student),null,200);assertEquals("00060001",profile.path("studentNo").asText());assertEquals(2026,profile.path("gradeYear").asInt());assertEquals("软件工程2班",profile.path("className").asText());
        assertTrue(confirm(d,"manual_confirm_1",200).path("replayed").asBoolean());assertEquals(student,confirm(d,"manual_confirm_2",200).path("studentId").asLong());
        assertEquals(1,studentCount());verify(students,times(1)).createStandaloneStudentProfile(any(CreateStandaloneStudentProfileRequest.class));
        assertEquals(users,db.queryForObject("SELECT COUNT(*) FROM user_account",Integer.class));assertEquals(accounts,db.queryForObject("SELECT COUNT(*) FROM auth_account",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='AGENT_CREATE_PROFILE' AND actor_id=? AND resource_id=?",Integer.class,actor,student));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM agent_message WHERE session_id=?",Integer.class,d.path("sessionId").asText()));
    }

    @Test void invalidFieldsCannotBecomeReadyOrWriteAStudent() throws Exception {
        var d=draft(Map.of("gradeYear",1800,"name","A","studentNo","00060002","major","CS"));
        assertEquals("COLLECTING",d.path("status").asText());assertEquals("gradeYear",d.path("fieldErrors").get(0).path("field").asText());confirm(d,"invalid_grade_1",409);
        for(var fields:List.of(Map.of("gradeYear","2026"),Map.of("gradeYear",2026.5),Map.of("studentNo",60002),Map.of("className","X".repeat(101)),Map.of("schoolId",school),Map.of("userId",actor)))
            send(patch("/api/agent/drafts/"+id(d)),Map.of("expectedVersion",1,"fields",fields),400);
        assertEquals(1,send(get("/api/agent/drafts/"+id(d)),null,200).path("version").asInt());assertEquals(0,studentCount());verifyNoInteractions(students);
    }

    @Test void cancellationExpiryAndLegacyCollectingStateArePersisted() throws Exception {
        var d=draft(Map.of("name","A"));
        db.update("UPDATE agent_student_draft SET status='EDITING' WHERE id=?",id(d));
        assertEquals("COLLECTING",send(get("/api/agent/drafts/"+id(d)),null,200).path("status").asText());
        assertEquals("COLLECTING",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(d)));
        assertEquals("CANCELLED",send(post("/api/agent/drafts/"+id(d)+"/cancel"),Map.of("expectedVersion",1),200).path("status").asText());
        assertEquals("CANCELLED",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(d)));confirm(d,"cancelled_confirm_1",409);
        var expired=draft(Map.of("name","B","studentNo","00060003","major","CS"));db.update("UPDATE agent_student_draft SET expires_at=DATE_SUB(NOW(),INTERVAL 1 HOUR) WHERE id=?",id(expired));
        assertEquals("EXPIRED",send(get("/api/agent/drafts/"+id(expired)),null,200).path("status").asText());
        assertEquals("EXPIRED",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(expired)));confirm(expired,"expired_confirm_1",409);
        assertEquals(0,studentCount());
    }

    @Test void duplicateStudentNumberIsCheckedAndDatabaseUniquenessRemainsTheFinalGuard() throws Exception {
        var first=draft(Map.of("name","A","studentNo","00060004","major","CS"));confirm(first,"duplicate_first_1",200);
        var duplicate=draft(Map.of("name","B","studentNo"," 00060004 ","major","CS"));assertEquals("COLLECTING",duplicate.path("status").asText());
        assertTrue(duplicate.path("fieldErrors").toString().contains("studentNo"));confirm(duplicate,"duplicate_second_1",409);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO student_profile(school_id,name,student_no,major) VALUES(?,'Duplicate','00060004','CS')",school));
        assertEquals(1,studentCount());assertEquals(1,actionCount());
    }

    @Test void permissionOwnershipAndCsrfAreRequiredEvenWhenNoModelIsConfigured() throws Exception {
        var d=draft(Map.of("name","A","studentNo","00060005","major","CS"));
        var foreign=account(school(),"SCHOOL_ADMIN");as(get("/api/agent/drafts/"+id(d)),foreign,"SCHOOL_ADMIN",null,404);
        var sameSchoolOther=account(school,"SCHOOL_ADMIN");as(get("/api/agent/drafts/"+id(d)),sameSchoolOther,"SCHOOL_ADMIN",null,404);
        var student=account(school,"STUDENT");as(post("/api/agent/drafts/"+id(d)+"/confirm").header("Idempotency-Key","permission_student_1"),student,"STUDENT",Map.of("expectedVersion",1),403);
        mvc.perform(post("/api/agent/sessions").with(user(username).roles("SCHOOL_ADMIN")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/agent/drafts/"+id(d))).andExpect(status().isUnauthorized());
        db.update("UPDATE auth_account SET enabled=FALSE WHERE username=?",username);confirm(d,"permission_revoked_1",403);
        assertEquals(0,studentCount());verifyNoInteractions(students);
    }

    @Test void auditFailureRollsBackCommittedRestAttemptAndTheSameKeyCanBeRetried() throws Exception {
        var d=draft(Map.of("name","A","studentNo","00060006","major","CS","gradeYear",2026,"className","回滚测试班"));
        doThrow(new IllegalStateException("simulated audit failure")).when(audit).audit(eq(actor),eq("AGENT_CREATE_PROFILE"),anyString(),anyLong(),anyString());
        confirm(d,"rollback_rest_1",500);
        assertEquals(0,studentCount());assertEquals(0,actionCount());assertEquals("READY",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(d)));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='CREATE' AND resource_type='student_profile' AND actor_id=?",Integer.class,actor));
        reset(audit);var result=confirm(d,"rollback_rest_1",200);assertTrue(result.path("created").asBoolean());assertEquals(1,studentCount());assertEquals(1,actionCount());
    }

    @Test void confirmationRejectsUnseenVersionsAndLegacyCreatedReplayRemainsCompatible() throws Exception {
        var original=draft(Map.of("name","A","studentNo","00060007","major","CS"));var updated=updateDraft(original,Map.of("name","B"));
        confirm(original,"old_version_rest_1",409);assertEquals(0,studentCount());var result=confirm(updated,"legacy_confirm_1",200);
        db.update("UPDATE agent_student_draft SET status='COMMITTED' WHERE id=?",id(updated));
        db.update("UPDATE agent_action_request SET result_json=REPLACE(result_json,'CREATED','COMMITTED') WHERE school_id=?",school);
        assertEquals("CREATED",confirm(updated,"legacy_confirm_1",200).path("status").asText());
        assertEquals("CREATED",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,id(updated)));
        assertEquals("B",db.queryForObject("SELECT name FROM student_profile WHERE id=?",String.class,result.path("studentId").asLong()));assertEquals(1,studentCount());
    }
}
