package com.efedorchenko.gptbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.util.annotation.Nullable;

import java.util.Optional;

@Component
public class Helper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static String write(@Nullable Object object) {
        if (object instanceof String string) {
            return string;
        }

        if (object instanceof Optional<?> optional) {
            if (optional.isEmpty()) {
                return "Optional.empty";
            }
            object = optional.get();
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException ignore) {
            return object == null ? null : object.toString();
        }
    }
}