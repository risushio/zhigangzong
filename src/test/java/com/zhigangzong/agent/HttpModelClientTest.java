package com.zhigangzong.agent;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpModelClientTest {
    @Test void sendsServerSideToolProtocolAndRejectsMalformedResponse() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var responses=new ArrayDeque<>(List.of("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}","invalid","{\"choices\":[]}"));
        server.createContext("/chat",exchange->{
            assertEquals("Bearer test-only",exchange.getRequestHeaders().getFirst("Authorization"));
            var body=AgentJson.object(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));assertTrue(body.path("tools").isArray());assertFalse(body.path("parallel_tool_calls").asBoolean());
            byte[] bytes=responses.remove().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {var p=new AgentProperties();p.setModelEnabled(true);p.setApiKey("test-only");p.setModel("test-model");p.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort()+"/chat");
            var client=new HttpModelClient(p);var messages=List.<Map<String,Object>>of(Map.of("role","user","content","test"));
            assertEquals("ok",client.complete(messages,AgentToolRegistry.schemas(),Duration.ofSeconds(2)).path("content").asText());
            assertThrows(RuntimeException.class,()->client.complete(messages,AgentToolRegistry.schemas(),Duration.ofSeconds(2)));
            assertThrows(RuntimeException.class,()->client.complete(messages,AgentToolRegistry.schemas(),Duration.ofSeconds(2)));
        } finally {server.stop(0);}
    }
    @Test void doesNotMakeNetworkCallsWithoutConfiguration() {
        var client=new HttpModelClient(new AgentProperties());assertThrows(RuntimeException.class,()->client.complete(List.of(),List.of(),Duration.ofSeconds(1)));
    }
    @Test void boundsSlowAndOversizedResponses()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/slow",exchange->{try {Thread.sleep(300);exchange.sendResponseHeaders(200,2);exchange.getResponseBody().write("{}".getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}finally{exchange.close();}});
        server.createContext("/large",exchange->{try {byte[] bytes=new byte[140000];exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception ignored){}finally{exchange.close();}});
        server.start();
        try {
            var p=new AgentProperties();p.setModelEnabled(true);p.setApiKey("test-only");p.setModel("test-model");p.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort()+"/slow");var client=new HttpModelClient(p);
            var messages=List.<Map<String,Object>>of(Map.of("role","user","content","test"));
            var timeout=assertThrows(com.zhigangzong.exception.BusinessException.class,()->client.complete(messages,AgentToolRegistry.schemas(),Duration.ofMillis(30)));assertEquals("MODEL_TIMEOUT",timeout.getCode());
            p.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort()+"/large");assertThrows(com.zhigangzong.exception.BusinessException.class,()->client.complete(messages,AgentToolRegistry.schemas(),Duration.ofSeconds(2)));
        }finally{server.stop(0);}
    }
}
