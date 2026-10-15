package com.zhigangzong.agent.extraction;

import com.zhigangzong.agent.llm.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_STUDENT_EXTRACTION_LIVE_TESTS",matches="true")
class StudentExtractionLiveTest {
    static Stream<StudentExtractionFixtures.Sample> samples(){String ids=System.getProperty("live.extraction.ids");return StudentExtractionFixtures.samples().filter(sample->ids==null||java.util.Arrays.asList(ids.split(",")).contains(sample.id()));}
    @ParameterizedTest @MethodSource("samples")
    void extractsFixedFictionalChineseSampleUsingConfiguredProvider(StudentExtractionFixtures.Sample sample){
        String profile=System.getProperty("live.llm.profile","ollama");
        var runner=LlmProfileTestSupport.profile(profile).withPropertyValues("app.llm.enabled=true");
        String endpoint=System.getProperty("live.llm.endpoint");if(endpoint!=null)runner=runner.withPropertyValues("app.llm.endpoint="+endpoint);
        runner.run(context->{assertNull(context.getStartupFailure());
            LlmClient diagnostic=request->{var response=context.getBean(LlmClient.class).chat(request);
                try{var path=java.nio.file.Path.of(".local","agent-phase3","fictional-extraction-responses");java.nio.file.Files.createDirectories(path);java.nio.file.Files.writeString(path.resolve(sample.id()+".json"),response.content()==null?"null":response.content());}catch(java.io.IOException error){throw new AssertionError("Cannot write fictional fixture diagnostics");}return response;};
            var result=new StudentTextExtractor(diagnostic).extract(sample.input(),StudentExtractionFixtures.current(),Duration.ofSeconds(120));
            var expected=StudentTextExtractor.parse(sample.input(),new LlmResponse(sample.json(),java.util.List.of(),"stop",null,"fixture"));
            assertEquals(expected.multipleStudents(),result.multipleStudents(),sample.id());
            assertEquals(expected.patch().toDraftPatch(),result.patch().toDraftPatch(),sample.id());assertEquals(expected.patch().departmentName(),result.patch().departmentName(),sample.id());
            System.out.println("PASS live extraction "+profile+" "+sample.id()+"; fictional fixture, no database writes");
        });
    }
}
