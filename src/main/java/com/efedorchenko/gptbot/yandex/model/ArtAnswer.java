package com.efedorchenko.gptbot.yandex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Класс, представляющий ответ модели {@code YandexART}, который она предоставляет СРАЗУ после получения запроса,
 * то есть в нем не содержится сгенерированное изображение. Так как модель генерирует изображение только асинхронно,
 * суть этого класса - вернуть идентификатор (поле {@code id}), по которому клиент сможет опрашивать модель на
 * готовность своего изображения<br>
 * По окончании операции (т.е. когда {@code this.isDone == true}), результат включает либо поле с контентом
 * {@link ArtCompleteResponse} либо поля с ошибками {@link ArtAnswer#errorCode}/{@link ArtAnswer#errorDisc}
 */

@Getter
@ToString
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtAnswer implements Serializable {

    /** Идентификатор запущенного процесса по генерации изображения */
    @NotNull
    private final String id;

    /** Описание этого процесса генерации. Длина 0–256 символов. */
    @NotNull
    private final String description;

    /** Временная метка начала генерации, в текстовом представлении. Формат RFC3339 */
    @Nullable
    private final String createdAt;

    /** Идентификатор пользователя или учетной записи службы, инициировавшей процесс генерации */
    @NotNull
    private final String createdBy;

    /** Временная метка последнего изменения этого объекта моделью в процессе генерации. Формат RFC3339 */
    @Nullable
    private final String modifiedAt;

    /** Флаг готовности изображения */
    private final boolean done;

    /** Метаданные, относящиеся к службе, связанные с этим процессом генерации */
    @Nullable
    private final String metadata;

    /**
     * Код грубой ошибки. Возвращается с {@code HttpStatus = 4хх/5хх}<br>
     * Значение перечисления <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     */
    @Nullable
    @JsonProperty("code")
    private final Integer errorCode;

    /**
     * Грубая ошибка, например нарушение политики использования. Возвращается с {@code HttpStatus = 4хх/5хх}
     * Краткое пояснение к ошибке, подробнее см. <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     */
    @Nullable
    @JsonProperty("error")
    private final String errorDisc;

    /** Нормальный ответ от модели в случае успеха генерации. Содержит готовое изображение */
    @Nullable
    private final ArtCompleteResponse response;

    public boolean hasErrors() {
        return errorCode != null && errorDisc != null;
    }

    /**
     * Объект с результатом успешного процесса генерации, содержащий результат, закодированный в {@code Base64}
     */
    @Getter
    @ToString
    @AllArgsConstructor
    public static class ArtCompleteResponse {

        /** Сам результат операции - изображение в кодировке {@code Base64} */
        private final String image;

        /** Номер модели, сгенерировавшей это изображение */
        private final String modelVersion;

    }
}
