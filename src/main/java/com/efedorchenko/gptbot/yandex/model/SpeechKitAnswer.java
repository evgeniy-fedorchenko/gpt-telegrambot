package com.efedorchenko.gptbot.yandex.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Класс представляет ответ от нейросети {@code Yandex SpeechKit}. Содержит или результат успешного
 * преобразования в поле {@code result} (пустая строка в случае неразборчивой записи) или
 * ошибку, как техническое нарушение констрейнтов. Ограничения морали и этичности отсутствуют
 */
@Getter
@ToString
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpeechKitAnswer {

    /** Результат преобразования аудио сообщения в текст */
    private final String result;

    /** Http-статус ошибки. Например, {@code BAD_REQUEST} */
    @JsonProperty("error_code")
    private final String errorCode;

    /** Описание и причина ошибки */
    @JsonProperty("error_message")
    private final String errorMessage;

}
