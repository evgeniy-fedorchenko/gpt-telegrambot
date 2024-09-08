package com.efedorchenko.gptbot.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class DefaultBotAnswerConfiguration {

    private static final String ANSWERS_LOCATION = "classpath:default-bot-answers.yml";

    @Bean
    Properties defaultBotAnswers(ResourceLoader resourceLoader) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        Resource resource = resourceLoader.getResource(ANSWERS_LOCATION);
        yaml.setResources(resource);
        return yaml.getObject();
    }

}
