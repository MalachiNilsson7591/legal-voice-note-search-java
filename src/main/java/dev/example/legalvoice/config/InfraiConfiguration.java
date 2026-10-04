package dev.example.legalvoice.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import dev.example.legalvoice.infra.InfraiVectorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfraiConfiguration {
    @Bean
    OpenAIClient openAIClient(@Value("${infrai.api-key}") String apiKey,
                              @Value("${infrai.base-url}") String baseUrl) {
        return OpenAIOkHttpClient.builder().apiKey(apiKey).baseUrl(baseUrl).build();
    }

    @Bean
    InfraiVectorClient infraiVectorClient(@Value("${infrai.api-key}") String apiKey,
                                          @Value("${infrai.base-url}") String baseUrl,
                                          ObjectMapper mapper) {
        return new InfraiVectorClient(HttpClient.newHttpClient(), URI.create(baseUrl), apiKey, mapper);
    }
}
