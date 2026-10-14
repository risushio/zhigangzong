package com.zhigangzong.agent.llm;

import com.zhigangzong.agent.AgentProperties;
import com.zhigangzong.agent.config.LlmConfiguration;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

public final class LlmProfileTestSupport {
    private LlmProfileTestSupport() {}
    public static ApplicationContextRunner runner() {return new ApplicationContextRunner().withUserConfiguration(LlmConfiguration.class,AgentProperties.class);}
    public static ApplicationContextRunner profile(String name) {
        if(!name.equals("ollama")&&!name.equals("deepseek"))throw new IllegalArgumentException("Unknown live profile");
        var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application-"+name+".yml"));var values=yaml.getObject();
        return runner().withInitializer(context->context.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("llm-profile",values)));
    }
}
