package com.yiling.common.config;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    static class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        FlexibleLocalDateTimeDeserializer() { super(LocalDateTime.class); }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText().trim();
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
                return OffsetDateTime.parse(value).toLocalDateTime();
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        // modulesToInstall ADDS to Spring Boot's auto-detected modules (incl. JavaTimeModule from
        // jackson-datatype-jsr310 on the classpath) — using .modules(...) instead would REPLACE
        // that list and silently break all LocalDateTime (de)serialization across the app.
        return builder -> builder.modulesToInstall(new SimpleModule().addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer()));
    }
}
