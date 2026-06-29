package tech.lougon.profitly.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BrapiConfig {
    @Value("${brapi.base-url}")
    private String baseUrl;

    @Value("${brapi.token}")
    private String token;

    @Bean
    public WebClient brapiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}