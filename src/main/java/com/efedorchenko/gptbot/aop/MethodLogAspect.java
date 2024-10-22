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
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
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

    private final LogUtils logUtils;
    private final ExecutorService executorOfVirtual;

    @Around("@annotation(log)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Log log) throws Throwable {

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Logger logger = LoggerFactory.getLogger(method.getDeclaringClass().getName() + "." + method.getName());

        CompletableFuture<List<Object>> paramsForLogFuture = null;
        boolean enabledForLevel = logger.isEnabledForLevel(log.level());

        if (enabledForLevel) {

            paramsForLogFuture = CompletableFuture.supplyAsync(() -> {

                List<Object> methodParams = Arrays.asList(joinPoint.getArgs());
                List<Integer> excludes = Arrays.stream(log.exclude()).boxed().toList();

                return IntStream.range(0, methodParams.size())
                        .filter(i -> !excludes.contains(i))
                        .mapToObj(methodParams::get)
                        .toList();

            }, executorOfVirtual);
        }

        try {
            Object result = joinPoint.proceed();
            if (enabledForLevel) {
                logUtils.doLogMethodAsyncForSomeone(paramsForLogFuture, result, logger, log);
            }
            return result;

        } catch (Throwable ex) {
            if (enabledForLevel) {
                logUtils.doLogMethodAsyncForSomeone(paramsForLogFuture, ex, logger, log);
            }
            throw ex;
        }
    }

}
