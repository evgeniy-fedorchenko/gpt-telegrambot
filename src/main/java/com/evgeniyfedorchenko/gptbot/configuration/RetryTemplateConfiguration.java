package com.evgeniyfedorchenko.gptbot.configuration;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;

@Getter
@Setter
@Component
@Validated
@Configuration
@ConfigurationProperties(
        prefix = RetryTemplateConfiguration.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class RetryTemplateConfiguration {

    static final String CONFIGURATION_PREFIX = "retry";

    @Positive
    private int maxAttempts;
    @Positive
    private long backOffPeriodMillis;

    @Bean
    public RetryTemplate retryTemplate() {

        // TODO 07.08.2024 11:28 - Создать свое исключение под фейл на ретри
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxAttempts, Collections.singletonMap(Exception.class, true)
        );

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(backOffPeriodMillis);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

}
