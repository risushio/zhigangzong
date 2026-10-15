package com.zhigangzong.agent.extraction;

import com.zhigangzong.agent.llm.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class StudentTextExtractorTest {
    static Stream<StudentExtractionFixtures.Sample> samples(){return StudentExtractionFixtures.samples();}
    static LlmResponse response(String json){return new LlmResponse(json,List.of(),"stop",null,"fixture");}
    @ParameterizedTest @MethodSource("samples")
    void typedPatchesMapSyntheticChineseInputWithoutBusinessWrites(StudentExtractionFixtures.Sample sample){
        var result=new StudentTextExtractor(request->{assertEquals(LlmRequest.ResponseFormat.JSON_OBJECT,request.responseFormat());assertTrue(request.tools().isEmpty());assertEquals(sample.input(),request.messages().get(1).content());return response(sample.json());})
            .extract(sample.input(),StudentExtractionFixtures.current(),Duration.ofSeconds(2));
        assertEquals(sample.multiple(),result.multipleStudents());
        var expected=StudentTextExtractor.parse(sample.input(),response(sample.json())).patch();
        assertEquals(expected.toDraftPatch(),result.patch().toDraftPatch());assertEquals(expected.departmentName(),result.patch().departmentName());
    }
    @Test void nullDoesNotClearAndReturnedPatchIsDefensive(){
        var result=StudentTextExtractor.parse("专业软件工程",response("{\"multipleStudents\":false,\"fields\":{\"name\":null,\"phone\":null,\"majorName\":\"软件工程\"},\"clearFields\":[]}"));
        var patch=result.patch().toDraftPatch();assertFalse(patch.has("phone"));assertFalse(patch.has("name"));assertEquals("软件工程",patch.path("major").asText());
        patch.put("name","injected");assertFalse(result.patch().toDraftPatch().has("name"));assertFalse(result.patch().toString().contains("软件工程"));assertFalse(StudentExtractionFixtures.current().toString().contains("13800138000"));
    }
    @ParameterizedTest @ValueSource(strings={"{}","[]","not json","{} {}","{\"multipleStudents\":false,\"fields\":{\"studentNo\":123},\"clearFields\":[]}","{\"multipleStudents\":false,\"fields\":{\"gradeYear\":\"2026\"},\"clearFields\":[]}","{\"multipleStudents\":false,\"fields\":{\"departmentId\":\"1\"},\"clearFields\":[]}","{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[\"phone\",\"phone\"]}"})
    void refusesMalformedTypesIdsAndAmbiguousOperations(String json){assertThrows(LlmException.class,()->StudentTextExtractor.parse("学号123，年级2026，清空手机号",response(json)));}
    @Test void ungroundedValuesUnrequestedClearAndToolCallsFailSafely(){
        for(String json:List.of("{\"multipleStudents\":false,\"fields\":{\"phone\":\"13900139000\"},\"clearFields\":[]}","{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[\"phone\"]}"))
            assertThrows(LlmException.class,()->StudentTextExtractor.parse("保留当前手机号",response(json)));
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("不要清空手机号",response("{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[\"phone\"]}")));
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("资料",new LlmResponse(null,List.of(new LlmToolCall("x","create_student",LlmJson.object("{}"))),"tool_calls",null,"fixture")));
    }
    @Test void exceptionsStayRecoverableWithoutSensitiveData(){
        var extractor=new StudentTextExtractor(request->{throw new LlmException(LlmException.Code.TIMEOUT,"模型请求超时");});
        var error=assertThrows(LlmException.class,()->extractor.extract("虚构资料",StudentExtractionFixtures.current(),Duration.ofSeconds(1)));
        assertEquals(LlmException.Code.TIMEOUT,error.getCode());assertFalse(error.toString().contains("虚构资料"));
    }
    @Test void truncatedIdentifiersCannotLoseLeadingZerosOrBecomeASubstring(){
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("学号000123",response("{\"multipleStudents\":false,\"fields\":{\"studentNo\":\"123\"},\"clearFields\":[]}")));
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("手机号13800138000",response("{\"multipleStudents\":false,\"fields\":{\"phone\":\"13800\"},\"clearFields\":[]}")));
    }
    @Test void protocolRepairIsBoundedAndKeepsTheSameDeadline(){
        var calls=new java.util.concurrent.atomic.AtomicInteger();var budgets=new ArrayList<Duration>();
        var extractor=new StudentTextExtractor(request->{budgets.add(request.timeout());if(calls.incrementAndGet()==1)return response("not-json");return response("{\"multipleStudents\":false,\"fields\":{\"name\":\"林验\"},\"clearFields\":[]}");});
        assertEquals("林验",extractor.extract("姓名林验",StudentExtractionFixtures.current(),Duration.ofSeconds(2)).patch().toDraftPatch().path("name").asText());
        assertEquals(2,calls.get());assertTrue(budgets.get(1).compareTo(budgets.get(0))<=0);
        calls.set(0);var failed=new StudentTextExtractor(request->{calls.incrementAndGet();return response("not-json");});
        assertThrows(LlmException.class,()->failed.extract("姓名林验",StudentExtractionFixtures.current(),Duration.ofSeconds(2)));assertEquals(2,calls.get());
    }
    @Test void classInformationCannotInventADepartmentOrMajorAndIdentityIsNotSent(){
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("班级为Engineering",response("{\"multipleStudents\":false,\"fields\":{\"departmentName\":\"Engineering\"},\"clearFields\":[]}")));
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("班级软件工程1班",response("{\"multipleStudents\":false,\"fields\":{\"className\":\"软件工程1班\",\"majorName\":\"软件工程\"},\"clearFields\":[]}")));
        var result=new StudentTextExtractor(request->{assertFalse(request.messages().stream().anyMatch(message->message.content().contains("schoolId")||message.content().contains("draftId")||message.content().contains("userId")));return response("{\"multipleStudents\":false,\"fields\":{\"name\":\"周验\",\"phone\":\"13800138000\",\"studentNo\":\"000999\"},\"clearFields\":[]}");}).extract("姓名周验",StudentExtractionFixtures.current(),Duration.ofSeconds(2));
        assertEquals(1,result.patch().toDraftPatch().size());assertFalse(result.patch().toDraftPatch().has("phone"));assertFalse(result.patch().toDraftPatch().has("studentNo"));
    }
    @Test void explicitlyProvidedCorrectionCannotBeSilentlyIgnored(){
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("专业改为软件工程",response("{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[]}")));
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("手机号为123",response("{\"multipleStudents\":false,\"fields\":{\"phone\":null},\"clearFields\":[]}")));
    }
    @Test void missingValueDescriptionsPreserveFieldsAndClearCannotBeIgnored(){
        var result=StudentTextExtractor.parse("手机号暂未提供",response("{\"multipleStudents\":false,\"fields\":{\"phone\":\"手机号暂未提供\"},\"clearFields\":[]}"));assertTrue(result.patch().isEmpty());
        assertThrows(LlmException.class,()->StudentTextExtractor.parse("清空手机号",response("{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[]}")));
        var changed=StudentTextExtractor.parse("手机号清空，班级改为新班",response("{\"multipleStudents\":false,\"fields\":{\"className\":\"新班\"},\"clearFields\":[\"phone\"]}"));assertTrue(changed.patch().toDraftPatch().path("phone").isNull());assertEquals("新班",changed.patch().toDraftPatch().path("className").asText());
    }
    @Test void compoundClassCorrectionCannotRetainAnEchoedOldClass(){
        var current=StudentFields.fromDraft(LlmJson.object("{\"name\":\"林验\",\"studentNo\":\"000999\",\"major\":\"软件工程\",\"className\":\"软件验收1班\",\"phone\":\"13800138000\"}"),null);
        var invalid=new StudentTextExtractor(request->response("{\"multipleStudents\":false,\"fields\":{\"className\":\"软件验收1班\"},\"clearFields\":[\"phone\"]}"));
        assertThrows(LlmException.class,()->invalid.extract("修改上一名学生的班级名称为软件验收2班，并明确清空手机号。其余资料保持原样。",current,Duration.ofSeconds(2)));
    }
    @Test void classNumberCannotTurnSingleStudentCorrectionIntoABatch(){
        String input="修改上一名学生的班级名称为软件验收2班，并明确清空手机号。其余资料保持原样。";
        assertThrows(LlmException.class,()->StudentTextExtractor.parse(input,response("{\"multipleStudents\":true,\"fields\":{},\"clearFields\":[]}")));
        assertTrue(StudentTextExtractor.parse("录入两名学生",response("{\"multipleStudents\":false,\"fields\":{},\"clearFields\":[]}")).multipleStudents());
    }
}
