package com.zhigangzong;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.bootstrap.enabled=false")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS", matches="true")
class ManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void realLoginAndSecurityBoundaries() throws Exception {
        String name="auth-"+UUID.randomUUID().toString().substring(0,20);
        jdbc.update("INSERT INTO school(name,code) VALUES('Auth test',?)",name);
        Long school=jdbc.queryForObject("SELECT id FROM school WHERE code=?",Long.class,name);
        jdbc.update("INSERT INTO user_account(school_id,display_name,role) VALUES(?,'Auth test','SCHOOL_ADMIN')",school);
        Long id=jdbc.queryForObject("SELECT id FROM user_account WHERE school_id=?",Long.class,school);
        jdbc.update("INSERT INTO auth_account(user_id,username,password_hash) VALUES(?,?,?)",id,name,encoder.encode("integration-password"));
        mvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/jobs").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/enterprises").with(user("admin").roles("SCHOOL_ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username",name).param("password","wrong")).andExpect(status().isUnauthorized());
        var result=mvc.perform(post("/api/auth/login").with(csrf()).param("username",name).param("password","integration-password"))
            .andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(name)).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get("/api/catalog/jobs").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles="SCHOOL_ADMIN")
    void enterpriseAndJobWorkflowAndReviewLength() throws Exception {
        String name="Workflow-"+UUID.randomUUID();
        long enterprise=create("enterprises",Map.of("name",name,"creditCode",name.substring(0,25)));
        var body=Map.<String,Object>of("enterpriseId",enterprise,"title",name,"description","Integration workflow","city","Shanghai","headcount",2);
        long job=create("jobs",body);
        String route="/api/jobs/"+job;
        mvc.perform(post(route+"/publication").with(csrf()).contentType("application/json").content("{\"status\":\"PUBLISHED\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/enterprises/"+enterprise+"/review").with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("decision","APPROVED","note","审".repeat(481))))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/enterprises/"+enterprise+"/review").with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("decision","APPROVED","note","审".repeat(480))))).andExpect(status().isOk());
        mvc.perform(post(route+"/review").with(csrf()).contentType("application/json").content("{\"decision\":\"APPROVED\",\"note\":\"通过\"}")).andExpect(status().isOk());
        mvc.perform(post(route+"/publication").with(csrf()).contentType("application/json").content("{\"status\":\"PUBLISHED\"}")).andExpect(status().isOk());
        assertEquals("PUBLISHED",jdbc.queryForObject("SELECT publish_status FROM job_position WHERE id=?",String.class,job));
        mvc.perform(get("/api/catalog/jobs").param("q",name).param("city","Shanghai").param("status","APPROVED"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].enterpriseName").value(name));
        mvc.perform(put(route).with(csrf()).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.reviewStatus").value("PENDING")).andExpect(jsonPath("$.data.publishStatus").value("DRAFT"));
    }

    private long create(String resource,Map<String,Object> body) throws Exception {
        return json.readTree(mvc.perform(post("/api/"+resource).with(csrf()).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
    }
}
