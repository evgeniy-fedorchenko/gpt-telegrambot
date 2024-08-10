package com.evgeniyfedorchenko.gptbot.aop;

import com.evgeniyfedorchenko.gptbot.statistic.entity.BotUser;
import com.evgeniyfedorchenko.gptbot.statistic.repository.BotUserRepository;
import com.evgeniyfedorchenko.gptbot.telegram.TelegramBot;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
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


//    @Pointcut("execution(* com.evgeniyfedorchenko.gptbot.yandex.service.YandexGptService.newCall(..))")
//    public void aroundYandexGptCall() {
//    }

//    @Pointcut("execution(* com.evgeniyfedorchenko.gptbot.yandex.service.YandexArtServiceOld.newCall(..))")
//    public void aroundYandexArtCall() {
//    }

    @Pointcut("execution(* com.evgeniyfedorchenko.gptbot.yandex.service.YandexGptService.buildAndExecutePost(..))")
    public void aroundYandexGptRequest() {
    }

    @Transactional
    @Around("aroundYandexGptRequest()")
    public Object collectGptStatsReq(ProceedingJoinPoint joinPoint) throws Throwable {

        log.info("inside");
        Object[] args = joinPoint.getArgs();
        Object result = joinPoint.proceed();


        User currentUser = TelegramBot.localUser.get();
        CompletableFuture.runAsync(() -> {

            if (result instanceof Optional<?> resultOpt && resultOpt.isPresent() && resultOpt.get() instanceof GptAnswer gptAnswer) {


                BotUser botUser = botUserRepository.findById(currentUser.getId()).orElseGet(BotUser::new);

                botUser.setChatId(currentUser.getId());
                botUser.setUsername(currentUser.getUserName());
                botUser.setYaGptRequestCount(botUser.getYaGptRequestCount() + 1);

                botUser.setYaTokenSpent(botUser.getYaTokenSpent() + gptAnswer.result().usage().totalTokens());

                botUserRepository.save(botUser);
            }

        }, executorServiceOfVirtual);

        return result;
    }
}

