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
         * Это роль самой GPT-модели. Ее сообщения передаются под этой ролью
         */
        ASSISTANT("assistant"),

        /**
         * Хз, что это. Типа заголовок какой-то. Передается один раз в начале, но работает и без него
         */
        SYSTEM("system");

        private final String role;

    }

}
