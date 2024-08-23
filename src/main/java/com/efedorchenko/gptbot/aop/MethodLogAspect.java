package com.efedorchenko.gptbot.aop;

import com.efedorchenko.gptbot.utils.logging.Log;
import com.efedorchenko.gptbot.utils.logging.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MethodLogAspect {

    @Value("${logging.max-mess-length}")
    private int maxLength;
    private final ExecutorService executorServiceOfVirtual;
    private final LogUtils logUtils;

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
                logUtils.doLogMethodAsyncForSomeoneElse(paramsForLogFuture, result, logger, log, maxLength);
            }
            return result;

        } catch (Throwable ex) {
            if (enabledForLevel) {
                logUtils.doLogMethodAsyncForSomeoneElse(paramsForLogFuture, ex, logger, log, maxLength);
            }
            throw ex;
        }
    }

}
