package com.efedorchenko.gptbot.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MdcConfigurer {

    @Pointcut("execution(* com.efedorchenko.gptbot.telegram.TelegramUpdateHandler.handleUpdate(..))")
    public void aroundHandleUpdate() {
    }

    @Around("aroundHandleUpdate()")
    public Object mdcConfigure(ProceedingJoinPoint joinPoint) throws Throwable {

//        Создаем RqUID - кусок рандомного UUID + chatId юзера, сделавшего запрос (достать из параметра)
//        Забираем полностью юзера через getFrom() или откуда-то там

        return joinPoint.proceed();

//        MDC.clear()
    }
}
