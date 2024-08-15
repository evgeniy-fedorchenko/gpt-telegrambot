package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.exception.RetryAttemptNotReadyException;
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

    /**
     * Максимальное количество попыток выполнения операции.
     * Определяет, сколько раз будет предпринята попытка выполнить операцию, прежде чем возникнет исключение.
     */
    @Positive
    private int maxAttempts;

    /**
     * Период ожидания между попытками, исчисляется в {@link java.util.concurrent.TimeUnit#MILLISECONDS}
     * Определяет время ожидания перед следующей попыткой выполнения операции в случае неудачи.
     * Значение должно быть положительным целым числом, представляющим время в миллисекундах.
     */
    @Positive
    private long backOffPeriodMillis;

    @Bean
    public RetryTemplate retryTemplate() {

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxAttempts, Collections.singletonMap(RetryAttemptNotReadyException.class, true)
        );

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(backOffPeriodMillis);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

}
