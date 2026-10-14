package com.zhigangzong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhigangzong.agent.*;
import com.zhigangzong.mapper.ManagementMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"app.bootstrap.enabled=false","app.agent.enabled=true","app.agent.cleanup-enabled=false","app.agent.processing-mode=tools"})
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class AgentConcurrencyIntegrationTest {
    @Autowired AgentStore store;
    @Autowired AgentOrchestrator orchestrator;
    @Autowired StudentProfileCreationService creation;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean ModelClient model;
    @MockitoSpyBean ManagementMapper audit;
    long school,actor;String username;
    long insert(String sql,Object...args){db.update(sql,args);return db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    @BeforeEach void setup(){
        username="agent-it-"+UUID.randomUUID().toString().substring(0,16);
        school=insert("INSERT INTO school(name,code) VALUES('Agent isolated fixture',?)",username);
        actor=insert("INSERT INTO user_account(school_id,display_name,role) VALUES(?,'Agent fixture','SCHOOL_ADMIN')",school);
        db.update("INSERT INTO auth_account(user_id,username,password_hash) VALUES(?,?,'test-only')",actor,username);
    }
    @AfterEach void cleanup(){
        SecurityContextHolder.clearContext();reset(audit,model);
        // 仅移除此测试刚创建的学校资料；先核对标记，按外键顺序清理。
        assertEquals(username,db.queryForObject("SELECT code FROM school WHERE id=?",String.class,school));
        db.update("DELETE c FROM agent_tool_call c JOIN agent_message m ON m.id=c.message_id JOIN agent_session s ON s.id=m.session_id WHERE s.school_id=?",school);
        db.update("DELETE FROM agent_action_request WHERE school_id=?",school);
        db.update("DELETE FROM agent_student_draft WHERE school_id=?",school);
        db.update("DELETE m FROM agent_message m JOIN agent_session s ON s.id=m.session_id WHERE s.school_id=?",school);
        db.update("DELETE FROM agent_session WHERE school_id=?",school);
        db.update("DELETE FROM audit_log WHERE actor_id=?",actor);
        db.update("DELETE FROM student_profile WHERE school_id=?",school);
        db.update("DELETE FROM auth_account WHERE user_id=?",actor);
        db.update("DELETE FROM user_account WHERE school_id=?",school);
        db.update("DELETE FROM department WHERE school_id=?",school);
        db.update("DELETE FROM school WHERE id=?",school);
    }
    <T>T as(Supplier<T> supplier){
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username,"test-only",List.of(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN"))));
        try{return supplier.get();}finally{SecurityContextHolder.clearContext();}
    }
    Map<String,Object> draft(String no){return as(()->{String session=(String)store.createSession().get("sessionId");return store.patch(session,null,null,json.valueToTree(Map.of("name","学生","studentNo",no,"major","CS")));});}
    String id(Map<String,Object>d){return (String)d.get("draftId");}
    List<Object> race(Supplier<?> first,Supplier<?> second)throws Exception{
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try{var jobs=new ArrayList<Future<Object>>();for(var task:List.of(first,second))jobs.add(executor.submit(()->{ready.countDown();go.await();try{return as(task::get);}catch(Exception e){return e;}}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();var out=new ArrayList<Object>();for(var job:jobs)out.add(job.get(20,TimeUnit.SECONDS));return out;
        }finally{executor.shutdownNow();}
    }
    @Test void concurrentSameKeyAndDifferentKeysReturnOneStudent()throws Exception{
        var d=draft("000301");var results=race(()->store.confirm(id(d),1,"same_key_1"),()->store.confirm(id(d),1,"same_key_1"));assertTrue(results.stream().allMatch(Map.class::isInstance),()->results.toString());
        assertEquals(((Map<?,?>)results.get(0)).get("studentId"),((Map<?,?>)results.get(1)).get("studentId"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school));
        var different=race(()->store.confirm(id(d),1,"other_key_1"),()->store.confirm(id(d),1,"other_key_2"));assertTrue(different.stream().allMatch(Map.class::isInstance));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school));
    }
    @Test void twoSessionsWithSameStudentNumberHaveOnlyOneSuccessAndDatabaseEnforcesUniqueness()throws Exception{
        var a=draft("000302");var b=draft("000302");var results=race(()->store.confirm(id(a),1,"draft_a_1"),()->store.confirm(id(b),1,"draft_b_1"));
        assertEquals(1,results.stream().filter(Map.class::isInstance).count());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO student_profile(school_id,name,student_no,major) VALUES(?,'duplicate','000302','CS')",school));
    }
    @Test void editingRacingConfirmationNeverConfirmsUnseenVersion()throws Exception{
        var d=draft("000303");var results=race(()->store.confirm(id(d),1,"edit_race_1"),()->store.patch((String)d.get("sessionId"),id(d),1,json.valueToTree(Map.of("name","新姓名"))));
        assertEquals(1,results.stream().filter(Map.class::isInstance).count());var persisted=as(()->store.getDraft(id(d)));
        if("CREATED".equals(persisted.get("status")))assertEquals("学生",db.queryForObject("SELECT name FROM student_profile WHERE school_id=?",String.class,school));
        else assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school));
    }
    @Test void auditFailureRollsBackStudentDraftAndIdempotencyThenRetrySucceeds(){
        var d=draft("000304");doThrow(new IllegalStateException("simulated audit failure")).when(audit).audit(eq(actor),eq("AGENT_CREATE_PROFILE"),anyString(),anyLong(),anyString());
        assertThrows(RuntimeException.class,()->as(()->store.confirm(id(d),1,"rollback_1")));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE school_id=?",Integer.class,school));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM agent_action_request WHERE school_id=?",Integer.class,school));assertEquals("READY",as(()->store.getDraft(id(d))).get("status"));
        reset(audit);assertEquals("CREATED",as(()->store.confirm(id(d),1,"rollback_1")).get("status"));
    }
    @Test void concurrentMessageReplayDoesNotRunModelTwice()throws Exception{
        String session=as(()->(String)store.createSession().get("sessionId"));var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(model.complete(anyList(),anyList(),any())).thenAnswer(call->{entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return json.valueToTree(Map.of("role","assistant","content","done"));});
        var executor=Executors.newSingleThreadExecutor();
        try{var first=executor.submit(()->as(()->orchestrator.message(session,"concurrent_message","学生资料")));assertTrue(entered.await(10,TimeUnit.SECONDS));
            var repeated=as(()->orchestrator.message(session,"concurrent_message","学生资料"));assertEquals("PROCESSING",repeated.get("processingStatus"));release.countDown();assertEquals("SUCCEEDED",first.get(15,TimeUnit.SECONDS).get("processingStatus"));
            verify(model,times(1)).complete(anyList(),anyList(),any());
        }finally{release.countDown();executor.shutdownNow();}
    }
    @Test void directBusinessServiceRequiresTransactionAndCurrentPermission(){
        var fields=json.createObjectNode().put("name","A").put("studentNo","000305").put("major","CS");
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->as(()->creation.create(fields)));
        db.update("UPDATE auth_account SET enabled=FALSE WHERE user_id=?",actor);assertThrows(RuntimeException.class,()->as(()->store.createSession()));
        var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThrows(com.zhigangzong.exception.BusinessException.class,()->transaction.execute(status->as(()->creation.create(fields))));
    }
    @Test void migrationPreservesLegacyProfilesAndAddsStandaloneSchoolUniqueness()throws Exception {
        String table="agent_migration_fixture_"+UUID.randomUUID().toString().replace("-","");
        assertTrue(table.matches("agent_migration_fixture_[a-f0-9]{32}"));
        db.execute("CREATE TABLE "+table+" (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,student_no VARCHAR(40) NOT NULL,major VARCHAR(100) NOT NULL,UNIQUE KEY(user_id),FOREIGN KEY(user_id) REFERENCES user_account(id)) ENGINE=InnoDB");
        try {
            db.update("INSERT INTO "+table+"(user_id,student_no,major) VALUES(?,' 000401 ','CS')",actor);
            String script=new org.springframework.core.io.ClassPathResource("db/migrations/20261012-agent-profile.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8).replace("student_profile",table);
            try(var connection=Objects.requireNonNull(db.getDataSource()).getConnection()) {
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(script.getBytes(java.nio.charset.StandardCharsets.UTF_8)),java.nio.charset.StandardCharsets.UTF_8));
            }
            assertEquals(school,db.queryForObject("SELECT school_id FROM "+table+" WHERE user_id=?",Long.class,actor));
            assertEquals("Agent fixture",db.queryForObject("SELECT name FROM "+table+" WHERE user_id=?",String.class,actor));
            assertEquals("000401",db.queryForObject("SELECT student_no FROM "+table+" WHERE user_id=?",String.class,actor));
            long originalId=db.queryForObject("SELECT id FROM "+table+" WHERE user_id=?",Long.class,actor);
            String gradeScript=new org.springframework.core.io.ClassPathResource("db/migrations/20261012-student-grade-class.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8).replace("student_profile",table);
            try(var connection=Objects.requireNonNull(db.getDataSource()).getConnection()) {
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(gradeScript.getBytes(java.nio.charset.StandardCharsets.UTF_8)),java.nio.charset.StandardCharsets.UTF_8));
            }
            assertEquals(originalId,db.queryForObject("SELECT id FROM "+table+" WHERE user_id=?",Long.class,actor));
            assertNull(db.queryForObject("SELECT grade_year FROM "+table+" WHERE user_id=?",Integer.class,actor));
            assertNull(db.queryForObject("SELECT class_name FROM "+table+" WHERE user_id=?",String.class,actor));
            db.update("UPDATE "+table+" SET grade_year=2026,class_name='Migration fixture class' WHERE id=?",originalId);
            assertEquals(2026,db.queryForObject("SELECT grade_year FROM "+table+" WHERE id=?",Integer.class,originalId));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
            db.update("INSERT INTO "+table+"(school_id,name,student_no,major) VALUES(?,'Independent','000402','CS')",school);
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO "+table+"(school_id,name,student_no,major) VALUES(?,'Duplicate','000402','CS')",school));
        } finally {db.execute("DROP TABLE "+table);}
    }
}
