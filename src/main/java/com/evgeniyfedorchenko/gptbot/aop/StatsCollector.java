package com.evgeniyfedorchenko.gptbot.aop;

import com.evgeniyfedorchenko.gptbot.statistic.entity.BotUser;
import com.evgeniyfedorchenko.gptbot.statistic.repository.BotUserRepository;
import com.evgeniyfedorchenko.gptbot.telegram.TelegramBot;
import com.evgeniyfedorchenko.gptbot.yandex.model.ArtAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StatsCollector {

    private final ExecutorService executorServiceOfVirtual;
    private final BotUserRepository botUserRepository;

    @Pointcut("execution(* com.evgeniyfedorchenko.gptbot.service.AiModelService.buildAndExecutePost(..))")
    public void aroundAiModelService() {
    }

    @Transactional
    @AfterReturning(pointcut = "aroundAiModelService()", returning = "result")
    public Object collectYandexGptStats(Object result) {

        User currentUser = TelegramBot.localUser.get();
        CompletableFuture.runAsync(() -> {

            if (result instanceof Optional<?> resultOpt && resultOpt.isPresent()) {

                BotUser botUser = botUserRepository.findById(currentUser.getId()).orElseGet(() -> {
                    BotUser newUser = new BotUser();
                    newUser.setChatId(currentUser.getId());
                    newUser.setUsername(currentUser.getUserName());
                    newUser.setName(currentUser.getFirstName() + " " + currentUser.getLastName());
                    return newUser;
                });

                switch (resultOpt.get()) {
                    case GptAnswer gpt -> botUser.setYaGptRequestCount(botUser.getYaGptRequestCount() + 1);
                    case ArtAnswer art -> botUser.setYaArtRequestCount(botUser.getYaArtRequestCount() + 1);
                    default -> {
                    }
                }
                botUserRepository.save(botUser);
            }

        }, executorServiceOfVirtual);

        return result;
    }
}

