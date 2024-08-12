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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class MethodLogAspect {

    private final ExecutorService executorServiceOfVirtual;

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

    private void doLog(CompletableFuture<List<Object>> paramsForLogFuture,
                       BiConsumer<String, Object[]> logFunction,
                       @Nullable Throwable exThrownInMethod,
                       @Nullable Object result) {

        if (exThrownInMethod != null) {
            logFunction.accept("PARAMS: [{}] | THROWN: [{}]", new Object[]{paramsForLogFuture.join(), exThrownInMethod});
            return;
        }
        if (result instanceof CompletableFuture<?> futureResult) {
            futureResult.handleAsync((res, resBrokenInMethod) -> {

                if (resBrokenInMethod != null) {
                    logFunction.accept("PARAMS: [{}] | THROWN: [{}]", new Object[]{paramsForLogFuture.join(), resBrokenInMethod});
                } else {
                    logFunction.accept("PARAMS: [{}] | RESULT: [{}]", new Object[]{paramsForLogFuture.join(), res});
                }
                return null;
            }, executorServiceOfVirtual);

        }
        logFunction.accept("PARAMS: [{}] | RESULT: [{}]", new Object[]{paramsForLogFuture.join(), result});
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
