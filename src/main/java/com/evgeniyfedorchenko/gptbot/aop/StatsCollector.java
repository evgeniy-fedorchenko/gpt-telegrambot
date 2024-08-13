package com.evgeniyfedorchenko.gptbot.aop;

import com.evgeniyfedorchenko.gptbot.data.BotUserRepository;
import com.evgeniyfedorchenko.gptbot.entity.BotUser;
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

        if (result instanceof Optional<?> resultOpt && resultOpt.isPresent()) {

            User currentUser = TelegramBot.localUser.get();
            CompletableFuture.runAsync(() -> {

                BotUser botUser = botUserRepository.findById(currentUser.getId()).orElseGet(() -> {
                    BotUser newUser = new BotUser();
                    newUser.setChatId(currentUser.getId());
                    newUser.setUsername(currentUser.getUserName());
                    newUser.setName(currentUser.getFirstName() + " " + currentUser.getLastName());

                    log.info("New user! :)     Details: {}", newUser);
                    return newUser;
                });

                switch (resultOpt.get()) {
                    case GptAnswer gpt -> {
                        int tokensSpent = gpt.result().usage().totalTokens();
                        botUser.setYagptReqsToday(botUser.getYagptReqsToday() + 1);
                        botUser.setYagptReqsTotal(botUser.getYagptReqsTotal() + 1);
                        botUser.setTokensSpentToday(botUser.getTokensSpentToday() + tokensSpent);
                        botUser.setTokensSpentTotal(botUser.getTokensSpentTotal() + tokensSpent);
                    }
                    case ArtAnswer art -> {
                        botUser.setYaartReqsToday(botUser.getYaartReqsToday() + 1);
                        botUser.setYaartReqsTotal(botUser.getYaartReqsTotal() + 1);
                    }
                    default -> {
                    }
                }
                botUserRepository.save(botUser);

            }, executorServiceOfVirtual);
        }

        return result;
    }

}

