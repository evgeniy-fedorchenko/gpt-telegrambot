package com.efedorchenko.gptbot.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MethodLogAspect {

    @Value("${logging.max-mess-length}")
    private int maxLength;

    private final ExecutorService executorServiceOfVirtual;
    private final ObjectMapper objectMapper;
    private static final Pattern BASE64_PATTERN = Pattern.compile("[A-Za-z0-9+/]{4000,}={0,2}");
    private static final int MIN_BASE64_LENGTH = 5000;

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

        BiConsumer<String, String> logFunction = logFunction(logger, log.level());

        if (result instanceof Throwable throwable) {
            logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
            logFunction.accept(EX, throwable.toString());

        } else if (result instanceof CompletableFuture<?> resultFuture) {
            resultFuture.whenComplete((resultComplete, exceptionComplete) -> {

                if (exceptionComplete != null) {
                    logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
                    logFunction.accept(EX, exceptionComplete.toString());

                } else {
                    logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
                    if (log.result()) {
                        logFunction.accept(RETURN, logPrepare(resultComplete));
                    }
                }
            });

        } else {
            logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
            if (log.result()) {
                logFunction.accept(RETURN, logPrepare(result));
            }
        }
    }

    private String logPrepare(Object object) {
        try {
            return excludeBase64(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException ex) {
            log.error("Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return "Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage();
        }
    }

    private String logPrepare(CompletableFuture<List<Object>> runningFuture) {
        try {
            List<Object> objects = runningFuture.get();
            String[] strings = new String[objects.size()];

            for (int i = 0; i < objects.size(); i++) {
                strings[i] = excludeBase64(objectMapper.writeValueAsString(objects.get(i)));
            }
            return String.join(", ", strings);

        } catch (InterruptedException | ExecutionException | JsonProcessingException ex) {
            log.error("Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return "Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage();
        }
    }

    private BiConsumer<String, String> logFunction(Logger logger, Level level) {

        Function<String, String> shortenIt = args -> args.length() > maxLength
                ? "..." + args.substring(args.length() - maxLength)
                : args;

        return switch (level) {
            case TRACE -> (pattern, args) -> logger.trace(pattern, shortenIt.apply(args));
            case DEBUG -> (pattern, args) -> logger.debug(pattern, shortenIt.apply(args));
            case WARN  -> (pattern, args) -> logger.warn(pattern, shortenIt.apply(args));
            case ERROR -> (pattern, args) -> logger.error(pattern, shortenIt.apply(args));
            default    -> (pattern, args) -> logger.info(pattern, shortenIt.apply(args));
        };
    }

    public static String excludeBase64(String jsonString) {
        StringBuilder result = new StringBuilder(jsonString);
        Matcher matcher = BASE64_PATTERN.matcher(jsonString);

        while (matcher.find()) {
            String base64Candidate = matcher.group();
            if (base64Candidate.length() >= MIN_BASE64_LENGTH && isValidBase64(base64Candidate)) {
                result.replace(matcher.start(), matcher.end(), "<base64_encoding_string>");
                matcher.reset(result.toString());
            }
        }
        return result.toString();
    }

    private static boolean isValidBase64(String str) {
        if (str.length() % 4 != 0) {
            return false;
        }
        String start = str.substring(0, Math.min(100, str.length()));
        String end = str.substring(Math.max(0, str.length() - 100));

        return start.matches("^[A-Za-z0-9+/]+") && end.matches("[A-Za-z0-9+/]+=*$");
    }
}
