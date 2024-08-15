package com.efedorchenko.gptbot.aop;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для логирования методов - их параметров и возвращаемого значения.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Log {

    /**
     * Индексы параметров, которые необходимо исключить из логирования, начиная с нуля. Если пустой,
     * все параметры будут заблокированы. Если под указанным индексом нет параметра (например,
     * когда метод принимает 3 параметра, и указано {@code @Log(exclude = {2, 3}}), то будет
     * выброшено исключение {@link IndexOutOfBoundsException}
     */
    int[] exclude() default {};

    /**
     * Указывает, нужно ли включать возвращаемое значение в лог. По умолчанию {@code true}, то есть
     * возвращаемое значение будет залогировано. При отсутствии возвращаемого значения (в {@code void}
     * методах) любое значение параметра будет проигнорировано
     */
    boolean result() default true;

    /**
     * Уровень логирования для параметров и результата
     */
    Level level() default Level.TRACE;

}
