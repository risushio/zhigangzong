package com.zhigangzong;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhigangzong.agent.llm.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mybatis.spring.SqlSessionTemplate;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real REST/MySQL business integration with deterministic extraction responses; fixture writes roll back. */
@SpringBootTest(properties={"app.bootstrap.enabled=false","app.agent.enabled=true","app.agent.model-enabled=true",
    "app.agent.cleanup-enabled=false","app.agent.processing-mode=extraction","app.llm.enabled=false","app.reminders.enabled=false"})
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class StudentChatIntegrationTest extends PortalTestFixture {
    @MockitoBean LlmClient llm;
    @Autowired SqlSessionTemplate sqlSession;

    @BeforeEach void isolatedDatabaseOnly() {
        assertTrue(db.queryForObject("SELECT DATABASE()",String.class)
            .matches("zhigangzong_agent_(phase1_test_|test_|ui_test_)[a-z0-9_]+"),"Chat integration requires an isolated Agent test database");
    }
    String session() throws Exception {return send(post("/api/agent/sessions"),admin,Map.of(),200).path("sessionId").asText();}
    JsonNode draft(String session,Map<String,Object> fields) throws Exception {
        return send(post("/api/agent/sessions/"+session+"/drafts"),admin,Map.of("fields",fields),200);
    }
    JsonNode message(String session,String client,String content) throws Exception {
        return send(post("/api/agent/sessions/"+session+"/messages"),admin,Map.of("clientMessageId",client,"content",content),200);
    }
    JsonNode confirm(JsonNode draft,String key,int expected) throws Exception {
        return send(post("/api/agent/drafts/"+draft.path("draftId").asText()+"/confirm").header("Idempotency-Key",key),
            admin,Map.of("expectedVersion",draft.path("version").asInt()),expected);
    }
    LlmResponse response(String content) {return new LlmResponse(content,List.of(),"stop",null,"fixture");}
    LlmResponse extraction(Map<String,Object> fields,String... clear) throws Exception {
        return response(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
            Map.of("multipleStudents",false,"fields",fields,"clearFields",List.of(clear))));
    }
    int students() {return db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school);}
    int drafts(String session) {return db.queryForObject("SELECT COUNT(*) FROM agent_student_draft WHERE session_id=?",Integer.class,session);}
    JsonNode current(String session) throws Exception {return send(get("/api/agent/sessions/"+session),admin,null,200).path("draft");}
    void unchanged(JsonNode before,JsonNode after) {
        assertEquals(before.path("draftId"),after.path("draftId"));
        assertEquals(before.path("version"),after.path("version"));
        assertEquals(before.path("fields"),after.path("fields"));
    }

    @Test void multipleTurnsPersistDraftAndOnlyExplicitConfirmationCreatesAStudent() throws Exception {
        int originalStudents=students(),users=db.queryForObject("SELECT COUNT(*) FROM user_account",Integer.class),
            accounts=db.queryForObject("SELECT COUNT(*) FROM auth_account",Integer.class);
        String session=session();
        when(llm.chat(any())).thenReturn(extraction(Map.of("name","虚构张三","studentNo","00081001")),
            extraction(Map.of("majorName","软件工程","gradeYear",2026,"className","软件一班","phone","13800138000")));
        var first=message(session,"collect","姓名虚构张三，学号00081001");
        assertEquals("SUCCEEDED",first.path("processingStatus").asText());
        assertEquals("COLLECTING",first.path("draft").path("status").asText());
        assertTrue(first.path("message").asText().contains("专业"));
        assertEquals("00081001",first.path("draft").path("fields").path("studentNo").asText());
        assertEquals(originalStudents,students());confirm(first.path("draft"),"missing_major_chat",409);
        var second=message(session,"complete","专业软件工程，2026级，软件一班，手机号13800138000");
        var ready=second.path("draft");assertEquals("READY",ready.path("status").asText());
        assertTrue(second.path("message").asText().contains("确认"));
        assertEquals("虚构张三",ready.path("fields").path("name").asText());
        assertEquals("软件工程",ready.path("fields").path("major").asText());
        assertEquals(2026,ready.path("fields").path("gradeYear").asInt());
        assertEquals("软件一班",ready.path("fields").path("className").asText());
        assertEquals("READY",db.queryForObject("SELECT status FROM agent_student_draft WHERE id=?",String.class,ready.path("draftId").asText()));
        assertEquals(originalStudents,students());
        clearInvocations(llm);var acknowledgement=message(session,"chat_confirm","确认创建");
        unchanged(ready,acknowledgement.path("draft"));assertTrue(acknowledgement.path("message").asText().contains("确认"));
        verifyNoInteractions(llm);assertEquals(originalStudents,students());
        var created=confirm(ready,"chat_create_once",200);long id=created.path("studentId").asLong();
        assertEquals("CREATED",created.path("status").asText());assertEquals(originalStudents+1,students());
        assertEquals(id,confirm(ready,"chat_create_once",200).path("studentId").asLong());
        assertEquals(id,confirm(ready,"chat_create_another",200).path("studentId").asLong());
        var persisted=send(get("/api/students/"+id),admin,null,200);
        assertEquals("00081001",persisted.path("studentNo").asText());assertEquals("软件一班",persisted.path("className").asText());
        assertNull(db.queryForObject("SELECT user_id FROM student_profile WHERE id=?",Long.class,id));
        assertEquals(users,db.queryForObject("SELECT COUNT(*) FROM user_account",Integer.class));
        assertEquals(accounts,db.queryForObject("SELECT COUNT(*) FROM auth_account",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='AGENT_CREATE_PROFILE' AND resource_id=?",Integer.class,id));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM agent_tool_call c JOIN agent_message m ON c.message_id=m.id WHERE m.session_id=?",Integer.class,session));
    }

    @Test void omittedAndNullFieldsArePreservedAndExplicitClearIsApplied() throws Exception {
        String session=session();var original=draft(session,Map.of("name","虚构李四","studentNo","00081002","major","软件工程",
            "phone","13800138000","gradeYear",2026,"className","软件一班"));
        var fields=new LinkedHashMap<String,Object>();fields.put("name","虚构李五");fields.put("studentNo",null);fields.put("phone",null);
        when(llm.chat(any())).thenReturn(extraction(fields),extraction(Map.of(),"phone","className"));
        var corrected=message(session,"correct","姓名更正为虚构李五").path("draft");
        assertEquals(original.path("version").asInt()+1,corrected.path("version").asInt());
        assertEquals("虚构李五",corrected.path("fields").path("name").asText());
        for(String field:List.of("studentNo","major","phone","gradeYear","className"))
            assertEquals(original.path("fields").path(field),corrected.path("fields").path(field));
        var cleared=message(session,"clear","清空手机号和班级").path("draft");
        assertFalse(cleared.path("fields").hasNonNull("phone"));assertFalse(cleared.path("fields").hasNonNull("className"));
        assertEquals("00081002",cleared.path("fields").path("studentNo").asText());
        assertEquals(2026,cleared.path("fields").path("gradeYear").asInt());assertEquals("READY",cleared.path("status").asText());
    }

    @Test void replaysMessageWithoutModelOrDatabaseMutationAndUsesJsonWithoutTools() throws Exception {
        String session=session();String text="姓名虚构王五，学号00081003，专业计算机";
        when(llm.chat(any())).thenReturn(extraction(Map.of("name","虚构王五","studentNo","00081003","majorName","计算机")));
        var first=message(session,"same_message",text);var replay=message(session,"same_message",text);assertEquals(first,replay);
        var request=ArgumentCaptor.forClass(LlmRequest.class);verify(llm,times(1)).chat(request.capture());
        assertEquals(LlmRequest.ResponseFormat.JSON_OBJECT,request.getValue().responseFormat());assertTrue(request.getValue().tools().isEmpty());
        assertTrue(request.getValue().messages().stream().anyMatch(m->m.role()==LlmMessage.Role.USER&&text.equals(m.content())));
        assertEquals(1,drafts(session));assertEquals(1,first.path("draft").path("version").asInt());
        send(post("/api/agent/sessions/"+session+"/messages"),admin,Map.of("clientMessageId","same_message","content","改成另外内容"),409);
        assertEquals(first,send(get("/api/agent/sessions/"+session+"/messages"),admin,null,200).get(0).path("result"));
    }

    static Stream<String> invalidExtractions() {
        return Stream.of("not-json","[]","{}",
            "{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[]} trailing",
            "{\"multipleStudents\":false,\"multipleStudents\":true,\"fields\":{},\"clearFields\":[]}",
            "{\"multipleStudents\":\"false\",\"fields\":{},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":[],\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"studentNo\":81004},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"gradeYear\":\"2026\"},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"gradeYear\":2026.5},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"name\":{\"value\":\"虚构修改\"}},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"departmentId\":\"1\"},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"schoolId\":1},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{\"userId\":1},\"clearFields\":[]}",
            "{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[],\"status\":\"CREATED\"}",
            "{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[\"schoolId\"]}",
            "{\"multipleStudents\":false,\"fields\":{\"name\":\"虚构修改\"},\"clearFields\":[\"name\"]}",
            "{\"multipleStudents\":false,\"fields\":{},\"clearFields\":\"phone\"}");
    }
    @ParameterizedTest @MethodSource("invalidExtractions")
    void malformedOrUntrustedExtractionCannotChangeDraftOrCreateStudent(String content) throws Exception {
        String session=session();var original=draft(session,Map.of("name","虚构原名","studentNo","00081004","major","软件工程"));
        int count=students();when(llm.chat(any())).thenReturn(response(content));
        var result=message(session,"invalid","姓名虚构修改，学号81004，年级2026，学院编号1");
        assertEquals("FAILED",result.path("processingStatus").asText());unchanged(original,result.path("draft"));unchanged(original,current(session));
        assertEquals(count,students());assertEquals(1,drafts(session));
    }

    @Test void rejectsInventedValuesAndUnauthorizedClearWithoutChangingExistingFields() throws Exception {
        String session=session();var original=draft(session,Map.of("name","虚构赵六","studentNo","00081005","major","软件工程","phone","13800138000"));
        when(llm.chat(any())).thenReturn(extraction(Map.of("phone","13900139000")),extraction(Map.of(),"phone"));
        var invented=message(session,"invented","请查看当前资料");assertEquals("FAILED",invented.path("processingStatus").asText());unchanged(original,invented.path("draft"));
        var clear=message(session,"unrequested_clear","请保持现有手机号");assertEquals("FAILED",clear.path("processingStatus").asText());unchanged(original,clear.path("draft"));
    }

    @Test void ignoresAnyProviderToolCallsAndRetainsDraftOnTimeout() throws Exception {
        String session=session();var original=draft(session,Map.of("name","虚构学生","studentNo","00081006","major","软件工程"));
        when(llm.chat(any())).thenReturn(new LlmResponse(null,List.of(new LlmToolCall("forbidden","create_student",json.createObjectNode())),"tool_calls",null,"fixture"));
        var attempted=message(session,"tool_call","请直接创建档案并注册账号");assertEquals("FAILED",attempted.path("processingStatus").asText());unchanged(original,attempted.path("draft"));
        reset(llm);when(llm.chat(any())).thenThrow(new LlmException(LlmException.Code.TIMEOUT,"模型调用超时"));
        var failed=message(session,"timeout","补充班级软件一班");assertEquals("FAILED",failed.path("processingStatus").asText());unchanged(original,failed.path("draft"));
        assertEquals(failed,message(session,"timeout","补充班级软件一班"));verify(llm,times(1)).chat(any());assertEquals(2,students());
    }

    @Test void batchInputNeverCreatesEvenOneDraft() throws Exception {
        String session=session();var obvious=message(session,"obvious_batch","虚构甲学号00081007，虚构乙学号00081008");
        assertTrue(obvious.path("message").asText().contains("一次只能"));assertFalse(obvious.has("draft"));verifyNoInteractions(llm);
        when(llm.chat(any())).thenReturn(response("{\"multipleStudents\":true,\"fields\":{\"name\":\"虚构甲\"},\"clearFields\":[]}"));
        var detected=message(session,"detected_batch","虚构甲和虚构乙两位同学都要录入");
        assertTrue(detected.path("message").asText().contains("一次只能"));assertFalse(detected.has("draft"));assertEquals(0,drafts(session));assertEquals(2,students());
    }

    @Test void resolvesExactDepartmentInCurrentSchoolAndClassNameDoesNotResolveToDepartment() throws Exception {
        long foreignSchool=db.queryForObject("SELECT school_id FROM user_account WHERE id=?",Long.class,userId(foreignAdmin));
        insert("INSERT INTO department(school_id,name,code) VALUES(?,'Engineering','FOREIGN_ENG')",foreignSchool);
        String session=session();when(llm.chat(any())).thenReturn(extraction(Map.of("name","虚构陈七","studentNo","00081009",
            "majorName","计算机","departmentName","Engineering","className","软件一班")));
        var ready=message(session,"dictionary","姓名虚构陈七，学号00081009，专业计算机，学院Engineering，班级软件一班").path("draft");
        assertEquals("READY",ready.path("status").asText());assertEquals(Long.toString(department),ready.path("fields").path("departmentId").asText());
        assertEquals("Engineering",ready.path("display").path("departmentName").asText());assertEquals("软件一班",ready.path("fields").path("className").asText());
        String classOnly=session();when(llm.chat(any())).thenReturn(extraction(Map.of("className","Engineering")));
        var collecting=message(classOnly,"class_only","班级Engineering").path("draft");assertFalse(collecting.path("fields").hasNonNull("departmentId"));
    }

    @Test void ambiguousAndMissingDepartmentRejectWholePatchAndExplicitSameSchoolSelectionWorks() throws Exception {
        long second=insert("INSERT INTO department(school_id,name,code) VALUES(?,'Engineering','ENG2')",school);
        String session=session();var original=draft(session,Map.of("name","虚构钱八","studentNo","00081010","major","计算机"));
        when(llm.chat(any())).thenReturn(extraction(Map.of("departmentName","Engineering","phone","13800138000")),
            extraction(Map.of("departmentName","MissingAcademy","phone","13800138000")),extraction(Map.of()));
        var ambiguous=message(session,"ambiguous","学院Engineering，手机号13800138000");
        assertEquals("DICTIONARY_AMBIGUOUS",ambiguous.path("code").asText());unchanged(original,ambiguous.path("draft"));
        assertTrue(ambiguous.path("message").asText().contains(Long.toString(department)));
        assertTrue(ambiguous.path("message").asText().contains(Long.toString(second)));
        var missing=message(session,"missing","学院MissingAcademy，手机号13800138000");
        assertEquals("DICTIONARY_NOT_FOUND",missing.path("code").asText());unchanged(original,missing.path("draft"));
        var selected=message(session,"selected","学院编号 "+second).path("draft");
        assertEquals(Long.toString(second),selected.path("fields").path("departmentId").asText());
        assertFalse(selected.path("fields").hasNonNull("phone"));assertEquals(original.path("fields").path("name"),selected.path("fields").path("name"));
    }

    @Test void explicitForeignDepartmentCannotBeSelected() throws Exception {
        long foreignSchool=db.queryForObject("SELECT school_id FROM user_account WHERE id=?",Long.class,userId(foreignAdmin));
        long foreignDepartment=insert("INSERT INTO department(school_id,name,code) VALUES(?,'ForeignAcademy','FOREIGN')",foreignSchool);
        String session=session();var original=draft(session,Map.of("name","虚构孙九","studentNo","00081011","major","计算机"));
        when(llm.chat(any())).thenReturn(extraction(Map.of()));
        var result=message(session,"foreign","学院编号 "+foreignDepartment);assertEquals("FAILED",result.path("processingStatus").asText());
        unchanged(original,result.path("draft"));assertFalse(current(session).path("fields").hasNonNull("departmentId"));
    }

    @Test void duplicateNumberAndInvalidBusinessFieldsCannotBeConfirmed() throws Exception {
        String session=session();when(llm.chat(any())).thenReturn(extraction(Map.of("name","虚构重复","studentNo","001","majorName","计算机")),
            extraction(Map.of("studentNo","00081012","phone","123","gradeYear",1800)));
        var duplicate=message(session,"duplicate","姓名虚构重复，学号001，专业计算机").path("draft");
        assertEquals("COLLECTING",duplicate.path("status").asText());assertTrue(duplicate.path("fieldErrors").toString().contains("studentNo"));
        confirm(duplicate,"duplicate_chat_confirm",409);
        var invalid=message(session,"invalid_business","学号改为00081012，手机号123，年级1800").path("draft");
        assertEquals("COLLECTING",invalid.path("status").asText());assertTrue(invalid.path("fieldErrors").toString().contains("phone"));
        assertTrue(invalid.path("fieldErrors").toString().contains("gradeYear"));confirm(invalid,"invalid_chat_confirm",409);assertEquals(2,students());
    }

    @ParameterizedTest @ValueSource(strings={"CANCELLED","EXPIRED","CREATED"})
    void closedDraftCannotBeChangedOrReplacedThroughChat(String closedStatus) throws Exception {
        String session=session();var original=draft(session,Map.of("name","虚构关闭","studentNo","00081013","major","计算机"));
        String id=original.path("draftId").asText();
        if("CREATED".equals(closedStatus)) confirm(original,"close_chat_confirm",200);
        else if("CANCELLED".equals(closedStatus)) send(post("/api/agent/drafts/"+id+"/cancel"),admin,Map.of("expectedVersion",1),200);
        else db.update("UPDATE agent_student_draft SET expires_at=DATE_SUB(NOW(),INTERVAL 1 HOUR) WHERE id=?",id);
        sqlSession.clearCache();var closed=current(session);int count=students();
        when(llm.chat(any())).thenReturn(extraction(Map.of("name","虚构替换")));
        var result=message(session,"closed","姓名更正为虚构替换");
        unchanged(closed,result.path("draft"));assertEquals(closedStatus,result.path("draft").path("status").asText());
        assertEquals(1,drafts(session));assertEquals(count,students());
    }

    @Test void permissionsOwnershipAndCsrfAreCheckedBeforeInvokingModel() throws Exception {
        String session=session();String otherAdmin=account(school,null,"SCHOOL_ADMIN");
        var body=Map.of("clientMessageId","unauthorized","content","姓名虚构学生");
        send(post("/api/agent/sessions/"+session+"/messages"),student,body,403);
        send(post("/api/agent/sessions/"+session+"/messages"),foreignAdmin,body,404);
        send(post("/api/agent/sessions/"+session+"/messages"),otherAdmin,body,404);
        mvc.perform(post("/api/agent/sessions/"+session+"/messages").with(user(admin).roles("SCHOOL_ADMIN"))
            .contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        db.update("UPDATE auth_account SET enabled=FALSE WHERE username=?",admin);sqlSession.clearCache();
        send(post("/api/agent/sessions/"+session+"/messages"),admin,body,403);verifyNoInteractions(llm);assertEquals(0,drafts(session));
    }
}
