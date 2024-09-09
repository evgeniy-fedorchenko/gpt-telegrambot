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
import java.util.UUID;
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

    private final ExecutorService executorServiceOfVirtual;
    private final BotUserRepository botUserRepository;

    @Pointcut("execution(* com.efedorchenko.gptbot.service.AiModelService.buildAndExecutePost(..))")
    public void aiExecutingPointcut() {
    }

    @Transactional
    @AfterReturning(pointcut = "aiExecutingPointcut()", returning = "result")
    public Object collectAiUsesStats(Object result) {

        String inner = UUID.randomUUID().toString().substring(30, 36);
        log.trace(inner + "Collecting stats. Result: {}", result);
        if ((result instanceof Optional<?> resultOpt && resultOpt.isPresent())) {

            CompletableFuture.runAsync(() -> {


                String mdcUserAsString = MDC.get(MdcConfigurer.MDC_USER);
                log.trace(inner + " MdcUserAsString got from MDC. MdcUserAsString: {}", mdcUserAsString);
                if (mdcUserAsString == null) {
                    log.error(inner + LogUtils.LOGIC_MARKER, "mdcUser is null");
                    return;
                }
                MdcConfigurer.MdcUser currentUser = MdcConfigurer.MdcUser.fromString(mdcUserAsString);
                log.trace(inner + " MdcUser restored from string. currentUser: {}", currentUser);
                if (currentUser == null) {
                    log.error(inner + LogUtils.LOGIC_MARKER, "mdcUser can't recover from String!");


                } else {
                    log.trace(inner + " current user: {}", currentUser.toString().replaceAll("\n", ""));
                    Optional<BotUser> botUserOpt = botUserRepository.findById(Long.valueOf(currentUser.getId()));
                    BotUser botUser;


                    if (botUserOpt.isEmpty()) {
                        log.trace(inner + " User was not found in repository");
                        botUser = new BotUser();
                        botUser.setChatId(Long.parseLong(currentUser.getId()));
                        botUser.setUsername(extractUsername(currentUser.getUsername()));   // nullable
                        botUser.setName(extractName(currentUser));
                        log.trace(inner + " create new user. User: {}", botUser);
                    } else {
//                        Если юзер изменил firstname/lastname/username
                        botUser = botUserOpt.get();
                        botUser.setUsername(extractUsername(currentUser.getUsername()));   // nullable
                        botUser.setName(extractName(currentUser));
                    }


                    try {
                        log.trace(inner + " " + resultOpt.get());
                    } catch (Throwable ignored) {
                    }


                    switch (resultOpt.get()) {
                        case GptAnswer gpt -> {
                            int tokensSpent = Integer.parseInt(gpt.getResult().getUsage().getTotalTokens());
                            botUser.setYagptReqsToday(botUser.getYagptReqsToday() + 1);
                            botUser.setYagptReqsTotal(botUser.getYagptReqsTotal() + 1);
                            botUser.setTokensSpentToday(botUser.getTokensSpentToday() + tokensSpent);
                            botUser.setTokensSpentTotal(botUser.getTokensSpentTotal() + tokensSpent);
                            log.trace(inner + " Complete case \"GptAnswer gpt\". BotUser: {}", botUser);
                        }
                        case ArtAnswer art -> {
                            botUser.setYaartReqsToday(botUser.getYaartReqsToday() + 1);
                            botUser.setYaartReqsTotal(botUser.getYaartReqsTotal() + 1);
                            log.trace(inner + " Complete case \"ArtAnswer art\". BotUser: {}", botUser);

                        }
                        default -> log.info(inner + " Complete default case. BotUser: {}", botUser);
                    }


                    log.trace(inner + " user for save: {}", botUser);
                    BotUser saved = null;
                    try {
                        saved = botUserRepository.save(botUser);
                        log.trace(inner + " successfully saved user: {}", saved);
                    } catch (Throwable throwable) {
                        log.error(inner + " Throwable in repo.save. saved: {}", saved);
                        log.error(inner + " Throwable in repo.save. Ex: ", throwable);

                    }
                    log.trace(inner + " complete collect stats");
                }
            }, executorServiceOfVirtual);


        } else {
            log.error(inner + LogUtils.LOGIC_MARKER, "Cannot get result. Result: {}", result);
        }

        return result;
    }

    @Nullable
    private String extractUsername(String username) {
        return username == null || username.equals("null") ? null : username;
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
        String replacedAll = replacedFirstname + replacedLastName;
        return replacedAll.isEmpty() ? null : replacedFirstname + " " + replacedLastName;
    }
}
