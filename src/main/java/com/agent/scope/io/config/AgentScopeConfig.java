package com.agent.scope.io.config;

import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentScopeConfig {

    @Value("${agentscope.openai.api-key}")
    private String apiKey;

    @Value("${agentscope.openai.model-name:gpt-4o-mini}")
    private String modelName;

    @Value("${agentscope.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Bean
    public OpenAIChatModel openAIChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }
}