package com.rc1.mantra_svc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Explicitly wires the application {@link ObjectMapper} (with JavaTimeModule,
 * no-timestamp dates) into WebFlux HTTP message codecs so that reactive request
 * body decoding (e.g. {@code Instant} fields) uses the same configuration as
 * the rest of the application.
 *
 * <p>Without this, Spring WebFlux's default codec auto-configuration may create
 * its own {@link ObjectMapper} that lacks {@code JavaTimeModule}, causing 400
 * Bad Request errors when deserialising {@code Instant} fields from ISO-8601
 * strings.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebFluxConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(
                new Jackson2JsonEncoder(objectMapper));
        configurer.defaultCodecs().jackson2JsonDecoder(
                new Jackson2JsonDecoder(objectMapper));
        // Allow reasonably-sized request bodies (16 MB)
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
    }

    /**
     * Exposes a {@link WebClient.Builder} bean so that services such as
     * {@code NewsService} can inject it for outbound HTTP calls.
     * Spring Boot's WebClient auto-configuration registers this automatically
     * in most setups, but declaring it explicitly guarantees availability when
     * the auto-configuration order is unpredictable.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
