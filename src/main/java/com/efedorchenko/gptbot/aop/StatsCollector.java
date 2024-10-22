package com.efedorchenko.gptbot.aop;

import com.efedorchenko.gptbot.data.BotUserRepository;
import com.efedorchenko.gptbot.entity.BotUser;
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
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StatsCollector {

    private static final Pattern allowedSymbols = Pattern.compile("[^A-Za-zА-Яа-я0-9._-]");

    private final ExecutorService executorOfVirtual;
    private final BotUserRepository botUserRepository;

    @Pointcut("execution(* com.efedorchenko.gptbot.service.AiModelService.buildAndExecutePost(..))")
    public void aiExecutingPointcut() {
    }

    @Transactional
    @AfterReturning(pointcut = "aiExecutingPointcut()", returning = "result")
    public Object collectAiUsesStats(Object result) {

        if (!(result instanceof Optional<?> resultOpt && resultOpt.isPresent())) {
            log.error("result in not instance of Optional<?> or is not present. Result: {}", result);
            return result;
        }

        CompletableFuture.runAsync(() ->
                Optional.ofNullable(MDC.get(MdcConfigurer.MDC_USER))
                        .map(MdcConfigurer.MdcUser::fromString)

                        .ifPresent(mdcUser -> {

//                      Только если result - это непустой Optional и в MDC есть юзер и его удалось восстановить из строки
                            BotUser botUser = botUserRepository.findById(Long.parseLong(mdcUser.getId()))
                                    .orElseGet(() -> {
                                        BotUser botUser1 = new BotUser();
                                        botUser1.setChatId(Long.parseLong(mdcUser.getId()));
                                        return botUser1;
                                    }).toBuilder()
                                    .username(extractUsername(mdcUser.getUsername()))
                                    .name(extractName(mdcUser))
                                    .build();

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
                                default -> log.warn("Unknown answer: {}", resultOpt.get());
                            }
                            botUserRepository.save(botUser);

                        }), executorOfVirtual);

        return result;
    }

    @Nullable
    private String extractUsername(String username) {
        return username == null || "null".equals(username) ? null : username;
    }

    @Nullable
    private String extractName(MdcConfigurer.MdcUser currentUser) {
        if (currentUser == null) {
            return null;
        }

        String firstname = currentUser.getFirstname();
        String lastname = currentUser.getLastname();
        Function<String, Boolean> isInvalid = string -> string == null || string.equals("null") || string.isBlank();

        Boolean firstNameIsInvalid = isInvalid.apply(firstname);
        Boolean lastNameIsInvalid = isInvalid.apply(lastname);

        if (firstNameIsInvalid && lastNameIsInvalid) {
            return null;
        }
        if (firstNameIsInvalid) {
            String replaced = allowedSymbols.matcher(lastname).replaceAll("");
            return replaced.isEmpty() ? null : replaced;
        }
        if (lastNameIsInvalid) {
            String replaced = allowedSymbols.matcher(firstname).replaceAll("");
            return replaced.isEmpty() ? null : replaced;
        }

        String replacedFirstname = allowedSymbols.matcher(firstname).replaceAll("");
        String replacedLastName = allowedSymbols.matcher(lastname).replaceAll("");
        return (replacedFirstname + replacedLastName).isEmpty() ? null : replacedFirstname + " " + replacedLastName;
    }
}
