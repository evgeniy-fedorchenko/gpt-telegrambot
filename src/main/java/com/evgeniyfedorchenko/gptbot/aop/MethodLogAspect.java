package com.evgeniyfedorchenko.gptbot.aop;

import jakarta.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static final String PARAMS = "-> [{}]";
    private static final String RETURN = "<- [{}]";
    private static final String EX = "!- [{}]";

    @Around("@annotation(log)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Logger logger = LoggerFactory.getLogger(signature.getMethod().getDeclaringClass());

        CompletableFuture<List<Object>> paramsForLogFuture = CompletableFuture.supplyAsync(() -> {

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

        Object result = null;
        Throwable thrown = null;
        BiConsumer<String, Object[]> logFunction = logFunction(logger, log.level());

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            thrown = ex;
            this.doLog(paramsForLogFuture, logFunction, thrown, result);
            throw ex;
        }

        this.doLog(paramsForLogFuture, logFunction, thrown, result);
        return result;
    }

    @Async("executorServiceOfVirtual")
    protected void doLog(CompletableFuture<List<Object>> paramsForLogFuture,
                         BiConsumer<String, Object[]> logFunction,
                         @Nullable Throwable exThrownInMethod,
                         @Nullable Object result) {

            if (exThrownInMethod != null) {
                logFunction.accept(PARAMS, new Object[]{getSafety(paramsForLogFuture)});
                logFunction.accept(EX, new Object[]{exThrownInMethod});
                return;
            }
            if (result instanceof CompletableFuture<?> resultFuture) {
                resultFuture.whenComplete((resF, exF) -> {
                    if (exF != null) {
                        logFunction.accept(PARAMS, new Object[]{getSafety(paramsForLogFuture)});
                        logFunction.accept(EX, new Object[]{exF});
                    } else {
                        logFunction.accept(PARAMS, new Object[]{getSafety(paramsForLogFuture)});
                        logFunction.accept(RETURN, new Object[]{resF});
                    }
                });
                return;
            }
            logFunction.accept(PARAMS, new Object[]{getSafety(paramsForLogFuture)});
            logFunction.accept(RETURN, new Object[]{result});
    }

    private List<Object> getSafety(CompletableFuture<List<Object>> runningFuture) {
        try {
            return runningFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            log.error("Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return Collections.singletonList("Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage());
        }
    }

    private BiConsumer<String, Object[]> logFunction(Logger logger, Level level) {
        return switch (level) {
            case TRACE -> logger::trace;
            case DEBUG -> logger::debug;
            case WARN -> logger::warn;
            case ERROR -> logger::error;
            default -> logger::info;
        };
    }
}
