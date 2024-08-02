package com.evgeniyfedorchenko.gptbot.yandex.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * История сообщений. Тот контекст, который должна "помнить" модель. Нужно передавать его в каждом запросе
 *
 * @param role Роль отправителя данного сообщения. Чтоб модель понимала, что отвечала она, а что юзер
 * @param text Сам текст сообщения
 */

public record GptMessageUnit(String role, String text) {

    /**
     * Идентификаторы отправителей сообщений. Передаются в объекте {@link GptMessageUnit}. Чтоб модель видела
     * что спрашивал сам юзер и что отвечала она - контекст и отталкивалась от него в будущих ответах.
     */
    @Getter
    @AllArgsConstructor
    public enum Role {

        /**
         * Роль юзера, который общается с моделью
         */
        USER("user"),

        /**
         * Роль самой GPT-модели. Ее сообщения передаются под этой ролью
         */
        ASSISTANT("assistant"),

        /**
         * Системная роль. Используется один раз в начале диалога для задания контекста поведения модели
         */
        SYSTEM("system");

        private final String role;

    }

}
