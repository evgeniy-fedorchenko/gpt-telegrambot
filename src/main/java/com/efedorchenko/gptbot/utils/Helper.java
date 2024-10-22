package com.efedorchenko.gptbot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.util.annotation.Nullable;

@Component
public class Helper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static @NotNull String write(@Nullable Update update) {
        try {
            return OBJECT_MAPPER.writeValueAsString(update);
        } catch (JsonProcessingException ignore) {
            return "<cannot serialize>. Use 'toString()': " + (update == null ? null : update.toString());
        }
    }

}
