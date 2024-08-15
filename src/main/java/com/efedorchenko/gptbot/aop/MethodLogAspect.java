package com.efedorchenko.gptbot.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class MethodLogAspect {

    private final ExecutorService executorServiceOfVirtual;
    private final ObjectMapper objectMapper;

    private static final String PARAMS = "-> [{}]";
    private static final String RETURN = "<- [{}]";
    private static final String EX = "!- [{}]";

    @Around("@annotation(log)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Log log) throws Throwable {


        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Logger logger = LoggerFactory.getLogger(method.getDeclaringClass().getName() + "." + method.getName());

        CompletableFuture<List<Object>> paramsForLogFuture = null;
        boolean enabledForLevel = logger.isEnabledForLevel(log.level());

        if (enabledForLevel) {

            paramsForLogFuture = CompletableFuture.supplyAsync(() -> {

                Object[] params = joinPoint.getArgs();
                List<Integer> excludedIdxs = Arrays.stream(log.exclude()).boxed().toList();

                List<Object> paramsForLog = new ArrayList<>();
                IntStream.range(0, params.length)
                        .forEach(i -> {
                            if (!excludedIdxs.contains(i)) {
                                paramsForLog.add(params[i]);
                            }
                        });
                return paramsForLog;
            }, executorServiceOfVirtual);
        }

        try {
            Object result = joinPoint.proceed();
            if (enabledForLevel) {
                this.doLog(paramsForLogFuture, result, logger, log);
            }
            return result;

        } catch (Throwable ex) {
            if (enabledForLevel) {
                this.doLog(paramsForLogFuture, ex, logger, log);
            }
            throw ex;
        }
    }

    @Async("executorServiceOfVirtual")
    protected void doLog(CompletableFuture<List<Object>> paramsForLogFuture,
                         Object result,
                         Logger logger,
                         Log log) {

        BiConsumer<String, String[]> logFunction = logFunction(logger, log.level());

        if (result instanceof Throwable throwable) {
            logFunction.accept(PARAMS, getSafety(paramsForLogFuture));
            logFunction.accept(EX, new String[]{throwable.toString()});

        } else if (result instanceof CompletableFuture<?> resultFuture) {
            resultFuture.whenComplete((resF, exF) -> {
                if (exF != null) {
                    logFunction.accept(PARAMS, getSafety(paramsForLogFuture));
                    logFunction.accept(EX, new String[]{exF.toString()});
                } else {
                    logFunction.accept(PARAMS, getSafety(paramsForLogFuture));
                    if (log.result()) {
                        logFunction.accept(RETURN, new String[]{resF.toString()});
                    }
                }
            });

        } else { // TODO 14.08.2024 10:30 - проверить result.toString()
            logFunction.accept(PARAMS, getSafety(paramsForLogFuture));
            if (log.result()) {
                logFunction.accept(RETURN, new String[]{result.toString()});
            }
        }
    }

    private String[] getSafety(CompletableFuture<List<Object>> runningFuture) {
        try {
            return runningFuture.get().stream().map(obj -> {
                try {
                    String s = objectMapper.writeValueAsString(obj);
                    return s;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(String[]::new);
        } catch (InterruptedException | ExecutionException ex) {
            log.error("Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return new String[]{"Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage()};
        }
    }

    private BiConsumer<String, String[]> logFunction(Logger logger, Level level) {
        return switch (level) {
            case TRACE -> logger::trace;
            case DEBUG -> logger::debug;
            case WARN -> logger::warn;
            case ERROR -> logger::error;
            default -> logger::info;
        };
    }
}
