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
    public static final String YAART_PROCESS_KEY = "yaart_process.";

    @Bean
    private Properties defaultBotAnswers() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        Resource resource = resourceLoader.getResource("classpath:default-bot-answers.yml");
        yaml.setResources(resource);
        return yaml.getObject();
    }

    /* ---------- commands ---------- */
    public String startCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "start");
    }
    public String helpCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "help");
    }
    public String feedbackCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "feedback");
    }
    public String yagptCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "yagpt");
    }
    public String yaartCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "yaart");
    }
    public String unknownCommand() {
        return defaultBotAnswers().getProperty(COMMANDS_KEY + "unknown");
    }

    /* ---------- exceptions ---------- */
    public String illegalStateEx() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "illegal_state");
    }
    public String nullPointerEx() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "null_pointer");
    }
    public String retryAttemptNotReadyEx() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "retry_attempt_not_ready");
    }
    public String jsonProcessingEx() {
        return defaultBotAnswers().getProperty(EXCEPTIONS_KEY + "json_processing");
    }
    public String otherEx() {
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

    /* ---------- yaart process ---------- */
    public String requestAccepted() {
        return defaultBotAnswers().getProperty(YAART_PROCESS_KEY + "request_accepted");
    }
    public String unknownError() {
        return defaultBotAnswers().getProperty(YAART_PROCESS_KEY + "unknown_error");
    }

}
