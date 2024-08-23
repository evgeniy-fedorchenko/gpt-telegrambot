package com.efedorchenko.gptbot.configuration.properties;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@AllArgsConstructor
public class DefaultBotAnswer {

    private final ResourceLoader resourceLoader;

    private static final String COMMANDS_KEY = "commands.";
    private static final String EXCEPTIONS_KEY = "exceptions.";
    private static final String OTHERS_KEY = "others.";

    @Bean
    private Properties defaultBotAnswers() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        Resource resource = resourceLoader.getResource("classpath:default-bot-answers.yml");
        yaml.setResources(resource);
        return yaml.getObject();
    }

    /* ---------- commands ---------- */
    public String start() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "start");
    }
    public String help() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "help");
    }
    public String feedback() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "feedback");
    }
    public String yagpt() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "yagpt");
    }
    public String yaart() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "yaart");
    }
    public String unknown() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "unknown");
    }

    /* ---------- exceptions ---------- */
    public String illegalState() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "illegal_state");
    }
    public String nullPointer() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "null_pointer");
    }
    public String retryAttemptNotReady() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "retry_attempt_not_ready");
    }
    public String jsonProcessing() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "json_processing");
    }
    public String otherExs() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "other_exs");
    }

    /* ---------- others ---------- */
    public String invalidDataFormat() {
        return defaultBotAnswers().getProperty(OTHERS_KEY + "invalid_data_format");
    }
    public String artGenProcessing() {
        return defaultBotAnswers().getProperty(OTHERS_KEY + "art_gen_processing");
    }
    public String couldNotRecognizeVoice() {
        return defaultBotAnswers().getProperty(OTHERS_KEY + "could_not_recognize_voice");
    }
    public String voiceIsLongerThan30s() {
        return defaultBotAnswers().getProperty(OTHERS_KEY + "voice_is_longer_than_30s");
    }

}
