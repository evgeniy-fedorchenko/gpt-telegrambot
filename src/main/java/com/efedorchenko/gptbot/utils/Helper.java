package com.efedorchenko.gptbot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.util.annotation.Nullable;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class Helper {

    private static final ObjectMapper objectMapper = new ObjectMapper(); {{
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }}

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
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ignore) {
            log.warn("WARNNN   " + ignore);
            return object == null ? null : object.toString();
        }
    }
}