package com.efedorchenko.gptbot.aop;

import com.efedorchenko.gptbot.data.BotUserRepository;
import com.efedorchenko.gptbot.entity.BotUser;
import com.efedorchenko.gptbot.utils.logging.LogUtils;
import com.efedorchenko.gptbot.yandex.model.ArtAnswer;
import com.efedorchenko.gptbot.yandex.model.GptAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Pointcut("execution(* com.efedorchenko.gptbot.service.AiModelService.buildAndExecutePost(..))")
    public void aiExecutingPointcut() {
    }

    @Transactional
    @AfterReturning(pointcut = "aiExecutingPointcut()", returning = "result")
    public Object collectAiUsesStats(Object result) {

        if ((result instanceof Optional<?> resultOpt && resultOpt.isPresent())) {

            CompletableFuture.runAsync(() -> {

                String mdcUserAsString = MDC.get(MdcConfigurer.MDC_USER);
                if (mdcUserAsString == null) {
                    log.error(LogUtils.LOGIC_MARKER, "mdcUser is null");
                    return;
                }
                MdcConfigurer.MdcUser currentUser = MdcConfigurer.MdcUser.fromString(mdcUserAsString);
                if (currentUser == null) {
                    log.error(LogUtils.LOGIC_MARKER, "mdcUser can't recover from String!");
                } else {
                    log.trace("current user: {}", currentUser.toString().replaceAll("\n", ""));
                    BotUser botUser = botUserRepository.findById(Long.valueOf(currentUser.getId())).orElseGet(() -> {
                        BotUser newUser = new BotUser();
                        newUser.setChatId(Long.parseLong(currentUser.getId()));
                        newUser.setUsername(currentUser.getUsername());   // nullable
                        newUser.setName(extractName(currentUser));

                        log.info("New user! :)     Details: {}", newUser);
                        return newUser;
                    });

                    switch (resultOpt.get()) {
                        case GptAnswer gpt -> {
                            int tokensSpent = Integer.parseInt(gpt.getResult().getUsage().getTotalTokens());
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
                    log.trace("user for save: {}", botUser);
                    botUserRepository.save(botUser);
                }

            }, executorServiceOfVirtual);

        } else {
            log.error(LogUtils.LOGIC_MARKER, "Cannot get result. Result: {}", result);
        }

        return result;
    }

    @Nullable
    private String extractName(MdcConfigurer.MdcUser currentUser) {
        String firstName = currentUser.getFirstname();
        String lastName = currentUser.getLastname();

        if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
            return null;
        }
        if (firstName == null || firstName.isBlank()) {
            return lastName;
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

}
