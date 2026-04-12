package com.flashlearn.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Globalna sanityzacja danych wejściowych na poziomie deserializacji JSON.
 * Każdy String z requestu jest automatycznie oczyszczany z tagów HTML (XSS).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule htmlSanitizingModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StdDeserializer<>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                String value = p.getValueAsString();
                if (value == null) return null;
                return value.replaceAll("<[^>]*>", "").trim();
            }
        });
        return module;
    }
}
