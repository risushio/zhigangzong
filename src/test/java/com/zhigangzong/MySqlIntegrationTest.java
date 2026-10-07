package com.zhigangzong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(roles = "SCHOOL_ADMIN")
@SpringBootTest(properties = "app.bootstrap.enabled=false")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class MySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void connectsToRealMySqlAndAllModuleQueriesWork() throws Exception {
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).matches("^[0-9].*"));
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.tableCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(25)));
        for (String route : new String[] {"schools", "departments", "users", "students", "enterprises", "jobs", "job-favorites", "applications", "recruitment-events", "batches", "placements", "approvals", "materials", "reports", "attendance", "changes", "guidance", "alerts", "alert-follow-ups", "evaluations", "archives", "notifications", "workflow-rules", "audit-logs", "match-feedback"}) {
            mvc.perform(get("/api/catalog/" + route)).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
            mvc.perform(get("/api/" + route)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.total").isNumber());
            mvc.perform(get("/api/" + route + "/9223372036854775807"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
        mvc.perform(get("/api/modules")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(20));
        mvc.perform(get("/api/statistics/overview")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedPlacements").isNumber());
    }

    @Test
    void createsFoundationRecordsAndAuditInRollbackTransaction() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long school = create("/api/schools", Map.of("name", "Integration School", "code", "IT-" + suffix)).get("id").asLong();
        long department = create("/api/departments", Map.of("schoolId", school, "name", "Engineering", "code", "ENG")).get("id").asLong();
        long user = create("/api/users", Map.of("schoolId", school, "departmentId", department,
                "displayName", "Test Student", "role", "STUDENT")).get("id").asLong();
        JsonNode student = create("/api/students", Map.of("userId", user, "studentNo", "IT-" + suffix,
                "major", "Computer Science", "daysPerWeek", 5));
        assertEquals(user, student.get("userId").asLong());

        JsonNode enterprise = create("/api/enterprises", Map.of("name", "Integration Enterprise", "creditCode", "IT-" + suffix));
        assertEquals("PENDING", enterprise.get("reviewStatus").asText());
        JsonNode job = create("/api/jobs", Map.of("enterpriseId", enterprise.get("id").asLong(),
                "title", "Java Intern", "description", "Backend internship", "city", "Shanghai", "headcount", 2));
        assertEquals("DRAFT", job.get("publishStatus").asText());
        assertEquals("PENDING", job.get("reviewStatus").asText());
        create("/api/batches", Map.of("departmentId", department, "name", "Integration Batch",
                "startDate", "2027-01-01", "endDate", "2027-06-30"));
        mvc.perform(get("/api/jobs/" + job.get("id").asLong())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java Intern"));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE resource_type='job_position' AND resource_id=?",
                Integer.class, job.get("id").asLong()));
    }

    @Test
    void duplicateAndForeignKeyViolationsReturn409() throws Exception {
        String code = "IT-" + UUID.randomUUID().toString().substring(0, 8);
        create("/api/schools", Map.of("name", "Integration School", "code", code));
        mvc.perform(post("/api/schools").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("name", "Duplicate", "code", code))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
        mvc.perform(post("/api/jobs").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content(json.writeValueAsString(
                        Map.of("enterpriseId", Long.MAX_VALUE, "title", "Orphan", "description", "Invalid",
                                "city", "Shanghai", "headcount", 1))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @Test
    void invalidDatesAndPagingAreRejected() throws Exception {
        mvc.perform(post("/api/batches").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content("""
                {"departmentId":1,"name":"Invalid","startDate":"2027-06-30","endDate":"2027-01-01"}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(get("/api/jobs").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs").param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/not-found")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/jobs/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isMethodNotAllowed());
    }

    private JsonNode create(String route, Map<String, Object> body) throws Exception {
        String response = mvc.perform(post(route).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("data");
    }
}
