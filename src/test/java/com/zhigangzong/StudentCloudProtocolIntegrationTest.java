package com.zhigangzong;

import com.sun.net.httpserver.HttpServer;
import com.zhigangzong.agent.llm.LlmJson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Full Agent -> extractor -> real HTTP adapter -> MySQL chain using a local DeepSeek protocol fixture. */
@ActiveProfiles("deepseek")
@SpringBootTest(properties={"app.bootstrap.enabled=false","app.agent.enabled=true","app.agent.model-enabled=true",
    "app.agent.processing-mode=extraction","app.agent.cleanup-enabled=false","app.llm.enabled=true",
    "app.llm.api-key=fixture-only","app.llm.max-retries=0","app.reminders.enabled=false"})
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class StudentCloudProtocolIntegrationTest extends PortalTestFixture {
    static final AtomicInteger replyStatus=new AtomicInteger(200);
    static final AtomicReference<String> body=new AtomicReference<>(),authorization=new AtomicReference<>();
    static final HttpServer server=startServer();
    static HttpServer startServer(){try{
        var local=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        local.createContext("/chat",exchange->{body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String fields=LlmJson.write(Map.of("multipleStudents",false,"fields",Map.of("name","虚构云验","studentNo","00082001","majorName","软件工程","gradeYear",2026,"className","云验一班"),"clearFields",List.of()));
            String payload=replyStatus.get()==200?LlmJson.write(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("role","assistant","content",fields))))):"PRIVATE_PROVIDER_BODY";
            byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(replyStatus.get(),bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});local.start();return local;
    }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}}
    @DynamicPropertySource static void endpoint(DynamicPropertyRegistry properties){properties.add("app.llm.endpoint",()->"http://127.0.0.1:"+server.getAddress().getPort()+"/chat");}
    @AfterAll static void close(){server.stop(0);}
    @BeforeEach void ready(){replyStatus.set(200);body.set(null);authorization.set(null);assertTrue(db.queryForObject("SELECT DATABASE()",String.class).matches("zhigangzong_agent_(phase1_test_|test_|ui_test_)[a-z0-9_]+"));}
    @Test void sameAgentCreatesOnlyAfterConfirmationWithDeepseekHttpProtocol() throws Exception {
        String session=send(post("/api/agent/sessions"),admin,Map.of(),200).path("sessionId").asText();
        var result=send(post("/api/agent/sessions/"+session+"/messages"),admin,Map.of("clientMessageId","cloud_fixture","content","姓名虚构云验，学号00082001，专业软件工程，2026级，班级云验一班"),200);
        var draft=result.path("draft");assertEquals("READY",draft.path("status").asText());assertEquals("SUCCEEDED",result.path("processingStatus").asText());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=? AND student_no='00082001'",Integer.class,school));
        var request=LlmJson.object(body.get());assertEquals("deepseek-flash",request.path("model").asText());assertEquals("none",request.path("reasoning_effort").asText());assertFalse(request.has("tools"));assertEquals("json_object",request.path("response_format").path("type").asText());assertEquals("Bearer fixture-only",authorization.get());
        var created=send(post("/api/agent/drafts/"+draft.path("draftId").asText()+"/confirm").header("Idempotency-Key","cloud_fixture_confirm"),admin,Map.of("expectedVersion",draft.path("version").asInt()),200);
        assertEquals("CREATED",created.path("status").asText());assertEquals("云验一班",db.queryForObject("SELECT class_name FROM student_profile WHERE id=?",String.class,created.path("studentId").asLong()));
        assertNull(db.queryForObject("SELECT user_id FROM student_profile WHERE id=?",Long.class,created.path("studentId").asLong()));
    }
    @Test void providerAuthenticationFailurePreservesMysqlDraftAndHidesBody() throws Exception {
        String session=send(post("/api/agent/sessions"),admin,Map.of(),200).path("sessionId").asText();
        var original=send(post("/api/agent/sessions/"+session+"/drafts"),admin,Map.of("fields",Map.of("name","虚构原名","studentNo","00082002","major","计算机")),200);
        replyStatus.set(401);var result=send(post("/api/agent/sessions/"+session+"/messages"),admin,Map.of("clientMessageId","cloud_fail","content","班级为云验一班"),200);
        assertEquals("MODEL_AUTHENTICATION",result.path("code").asText());assertEquals("FAILED",result.path("processingStatus").asText());assertFalse(result.toString().contains("PRIVATE_PROVIDER_BODY"));
        assertEquals(original.path("fields"),result.path("draft").path("fields"));assertEquals(original.path("version"),result.path("draft").path("version"));
    }
}
