package com.efedorchenko.gptbot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class Helper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String write(Update update) {
        try {
            return OBJECT_MAPPER.writeValueAsString(update);
        } catch (JsonProcessingException ignore) {
            return "<cannot serialize>. Use 'toString()': " + update.toString();
        }
    }

}
