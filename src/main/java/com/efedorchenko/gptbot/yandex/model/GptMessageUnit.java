package com.efedorchenko.gptbot.yandex.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * История сообщений. Тот контекст, который должна "помнить" модель. Нужно передавать его в каждом запросе
 */
@Getter
@ToString
@AllArgsConstructor
public class GptMessageUnit implements Serializable {

    /** Роль отправителя данного сообщения. Чтоб модель понимала, что отвечала она, а что юзер */
    private final String role;

    /** Текстовое содержимое сообщения */
    private final String text;

    /**
     * Идентификаторы отправителей сообщений. Передаются в объекте {@link GptMessageUnit}. Чтоб модель видела
     * что спрашивал сам юзер и что отвечала она - контекст и отталкивалась от него в будущих ответах<br>
     * <b>Важно!</b><br>
     * Модель понимает на входе только роли, представленные в нижнем регистре. При отправке роли в верхнем регистре
     * будет получена ошибка. Для установки корректного значения используйте предоставленный геттер
     */
    @Getter
    @AllArgsConstructor
    public enum Role {

        /**
         * Роль для отправки пользовательских сообщений к модели
         */
        USER("user"),

        /**
         * Используется для ответов, которые генерирует модель. Ответы модели, помеченные
         * ролью {@code assistant} включаются в состав сообщения для сохранения контекста
         * беседы. Не передавайте сообщения пользователя с этой ролью.
         */
        ASSISTANT("assistant"),

        /**
         * Системная роль, используется один раз в начале диалога и позволяет
         * задать контекст запроса и определить поведение модели
         */
        SYSTEM("system");

        private final String role;

        @Override
        public String toString() {
            return this.name();
        }
    }

}
