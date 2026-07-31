package com.testp.tattooappointmentservice.Config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureAiConfig {
    @Value("${azure.openai.key}")
    String openAiKey;

    @Value("${azure.openai.endpoint}")
    String openAiEndpoint;

    @Bean
    public OpenAIClient openAIClient() {
        return new OpenAIClientBuilder().credential(new KeyCredential(openAiKey)).endpoint(openAiEndpoint).buildClient();
    }
}
