package com.zhigangzong.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentDraftRulesTest {
    final AgentMapper mapper=mock(AgentMapper.class);
    @Test void retainsLeadingZerosAndDistinguishesClearFromOmission() {
        var normalized=StudentDraftRules.normalize(AgentJson.object("{\"name\":\" 张三 \",\"studentNo\":\"000123\",\"phone\":null}"));
        assertEquals("张三",normalized.path("name").asText());assertEquals("000123",normalized.path("studentNo").asText());
        assertTrue(normalized.path("phone").isNull());assertFalse(normalized.has("major"));
    }
    @Test void rejectsIdentityInjectionUnknownFieldsAndWrongTypes() {
        for(String input:new String[]{"{\"schoolId\":1}","{\"userId\":1}","{\"studentNo\":123}","{\"departmentId\":1}","{\"daysPerWeek\":2.5}","{\"phone\":true}"})
            assertThrows(RuntimeException.class,()->StudentDraftRules.normalize(AgentJson.object(input)));
        assertThrows(RuntimeException.class,()->AgentJson.object("{\"name\":\"A\",\"name\":\"B\"}"));
    }
    @Test void missingRequiredDateErrorsAndOutOfRangeDictionaryKeepDraftUnready() {
        var fields=StudentDraftRules.normalize(AgentJson.object("{\"name\":\"张三\",\"studentNo\":\"000123\",\"major\":\"CS\",\"departmentId\":\"999\",\"phone\":\"123\",\"daysPerWeek\":8,\"availableFrom\":\"2027-02-30\"}"));
        var validation=StudentDraftRules.validate(fields,1,mapper);assertFalse(validation.ready());assertEquals(4,validation.fieldErrors().size());
        assertEquals(3,StudentDraftRules.validate(AgentJson.object("{}"),1,mapper).missingFields().size());
    }
    @Test void neverExposesCreationToolOrAccountFields() {
        var schema=AgentJson.write(AgentToolRegistry.schemas());assertFalse(schema.contains("create_student"));assertFalse(schema.contains("password"));assertFalse(schema.contains("actorId"));assertFalse(schema.contains("schoolId"));
    }
    @Test void gradeAndClassRetainTypesAndRejectInvalidYearWithoutGuessing() {
        var fields=StudentDraftRules.normalize(AgentJson.object("{\"name\":\"A\",\"studentNo\":\"000123\",\"major\":\"CS\",\"gradeYear\":2026,\"className\":\" 软件工程1班 \"}"));
        assertTrue(fields.path("gradeYear").isInt());assertEquals("软件工程1班",fields.path("className").asText());
        var request=StudentDraftRules.creationRequest(fields);assertEquals(2026,request.gradeYear());assertEquals("000123",request.studentNo());
        fields.put("gradeYear",1800);assertFalse(StudentDraftRules.validate(fields,1,mapper).ready());
        assertThrows(RuntimeException.class,()->StudentDraftRules.normalize(AgentJson.object("{\"gradeYear\":\"2026\"}")));
        assertThrows(RuntimeException.class,()->StudentDraftRules.normalize(AgentJson.object("{\"gradeYear\":2026.5}")));
    }
}
