package com.zhigangzong.agent.extraction;

import com.sun.net.httpserver.HttpServer;
import com.zhigangzong.agent.llm.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class StudentExtractionProfileHttpTest {
    @ParameterizedTest @ValueSource(strings={"ollama","deepseek"})
    void sameExtractorConsumesBothProfileHttpProtocols(String profile) throws Exception {
        var request=new AtomicReference<String>();var auth=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat",exchange->{request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String content=LlmJson.write(Map.of("multipleStudents",false,"fields",Map.of("name","林验","studentNo","000123","majorName","软件工程"),"clearFields",List.of()));
            byte[] bytes=LlmJson.write(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("role","assistant","content",content))))).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try{LlmProfileTestSupport.profile(profile).withPropertyValues("app.llm.enabled=true","app.llm.endpoint=http://127.0.0.1:"+server.getAddress().getPort()+"/chat","app.llm.api-key=fixture-only").run(context->{
            assertNull(context.getStartupFailure());var result=new StudentTextExtractor(context.getBean(LlmClient.class)).extract("姓名林验，学号000123，专业软件工程",StudentExtractionFixtures.current(),Duration.ofSeconds(3));
            assertEquals("000123",result.patch().toDraftPatch().path("studentNo").asText());assertEquals("软件工程",result.patch().toDraftPatch().path("major").asText());
            var body=LlmJson.object(request.get());assertFalse(body.has("tools"));assertEquals(profile.equals("ollama")?"json_schema":"json_object",body.path("response_format").path("type").asText());assertEquals("Bearer fixture-only",auth.get());
        });}finally{server.stop(0);}
    }
}
