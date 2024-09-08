package com.efedorchenko.gptbot.configuration.properties;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@RequiredArgsConstructor
public class DefaultBotAnswer {

    @Value("${app.version}")
    private String appVersion;

    private final Properties defaultBotAnswers;

    private static final String COMMANDS_KEY = "commands.";
    private static final String EXCEPTIONS_KEY = "exceptions.";
    private static final String OTHERS_KEY = "others.";
    private static final String AI_PROCESS_KEY = "ai_process.";


    /* ---------- commands ---------- */
    public String startCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "start");
    }
    public String helpCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "help").formatted(appVersion);
    }
    public String feedbackCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "feedback");
    }
    public String yagptCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "yagpt");
    }
    public String yaartCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "yaart");
    }
    public String unknownCommand() {
        return defaultBotAnswers.getProperty(COMMANDS_KEY + "unknown");
    }


    /* ---------- exceptions ---------- */
    public String illegalStateEx() {
        return defaultBotAnswers.getProperty(EXCEPTIONS_KEY + "illegal_state");
    }
    public String nullPointerEx() {
        return defaultBotAnswers.getProperty(EXCEPTIONS_KEY + "null_pointer");
    }
    public String retryAttemptNotReadyEx() {
        return defaultBotAnswers.getProperty(EXCEPTIONS_KEY + "retry_attempt_not_ready");
    }
    public String jsonProcessingEx() {
        return defaultBotAnswers.getProperty(EXCEPTIONS_KEY + "json_processing");
    }
    public String otherEx() {
        return defaultBotAnswers.getProperty(EXCEPTIONS_KEY + "other_exs");
    }


    /* ---------- others ---------- */
    public String invalidDataFormat() {
        return defaultBotAnswers.getProperty(OTHERS_KEY + "invalid_data_format");
    }
    public String couldNotRecognizeVoice() {
        return defaultBotAnswers.getProperty(OTHERS_KEY + "could_not_recognize_voice");
    }
    public String voiceIsLongerThan30s() {
        return defaultBotAnswers.getProperty(OTHERS_KEY + "voice_is_longer_than_30s");
    }
    public String subscribeForUse() {
        return defaultBotAnswers.getProperty(OTHERS_KEY + "subscribe_for_use");
    }


    /* ---------- ai process ---------- */
    public String yaartRequestAccepted() {
        return defaultBotAnswers.getProperty(AI_PROCESS_KEY + "yaart_request_accepted");
    }
    public String artGenProcessing() {
        return defaultBotAnswers.getProperty(AI_PROCESS_KEY + "art_gen_processing");
    }
    public String unknownError() {
        return defaultBotAnswers.getProperty(AI_PROCESS_KEY + "unknown_error");
    }
    public String yagptAnswerOfStatus(HttpStatus errorStatus) {
        return switch (errorStatus) {   // switch for scaling
            case HttpStatus.TOO_MANY_REQUESTS -> defaultBotAnswers.getProperty(AI_PROCESS_KEY + "too_many_requests");
            case HttpStatus.BAD_GATEWAY -> this.unknownError();
            default -> defaultBotAnswers.getProperty(AI_PROCESS_KEY + "unknown_error");
        };
    }
    public String yaartBadPrompt() {
        return defaultBotAnswers.getProperty(AI_PROCESS_KEY + "yaart_bad_prompt");
    }
}
