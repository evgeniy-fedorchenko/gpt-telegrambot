package com.efedorchenko.gptbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.util.annotation.Nullable;

import java.util.Optional;

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
//             Serialization of null always returns "null" (string representation)
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException ignore) {
            return object.toString();
        }
    }
}
