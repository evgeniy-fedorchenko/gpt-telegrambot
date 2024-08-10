package com.evgeniyfedorchenko.gptbot.yandex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Класс, представляющий ответ модели {@code YandexART}, который она предоставляет СРАЗУ после получения запроса,
 * то есть в нем не содержится сгенерированное изображение. Так как модель генерирует изображение только асинхронно,
 * суть этого класса - вернуть идентификатор (поле {@code id}), по которому юзер сможет опрашивать модель на
 * готовность своего изображения<br>
 * По окончании операции (т.е. когда {@code this.isDone == true}), результат включает только одно
 * из полей {@link Error} или {@link ArtCompleteResponse}
 */

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtAnswer {

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

    // TODO 10.08.2024 23:15: поправить структуру объекта (часть с Error    )
    /** Обычная ошибка, например "кончился лимит токенов". Присутствует только в случае провала */
//    @Nullable
//    private final Error error;

    /** Грубая ошибка, например нарушение политики использования. Возвращается с {@code HttpStatus = 4хх/5хх} */
    @Nullable
    @JsonProperty("error")
    private final String errorString;

    /**
     * Код грубой ошибки. Возвращается с {@code HttpStatus = 4хх/5хх}<br>
     * Значение перечисления <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     */
    @Nullable
    private final Integer code;

    /** Нормальный ответ от модели в случае успеха генерации. Содержит готовое изображение */
    @Nullable
    private final ArtCompleteResponse response;

    public boolean hasErrors() {
        return errorString != null;
    }


    /**
     * Объект ошибки, содержащий сведения, поясняющие, что именно пошло не так.
     * При наличии поля этого класса в объекте {@link ArtAnswer} поле {@link ArtAnswer#response}
     * недоступно (равно {@code null})
     *
     * @param code    Код ошибки. Значение перечисления <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     * @param message Краткое пояснение к ошибке, подробнее см. <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     * @param details Список сообщений, содержащих сведения об ошибке
     */
//    public record Error(int code, @NotBlank String message, @NotNull List<String> details) {
//    }

    /**
     * Объект с результатом успешного процесса генерации, содержащий результат, закодированный в {@code Base64}
     *
     * @param image        Сам результат операции - изображение в кодировке {@code Base64}
     * @param modelVersion Номер модели, сгенерировавшей это изображение. todo проверить
     */
    public record ArtCompleteResponse(String image, String modelVersion) { // TODO 10.08.2024 23:29: переделать в класс, чтоб не было нал-алармов
    }
}

/*

todo вставить пример
{
    "id": "fbviqsidaptcb1adl44i",
    "description": "string",
    "createdAt": "2024-08-03T19:34:12.759471248Z",
    "createdBy": "string",
    "modifiedAt": "string",
    "done": true,
    "metadata": "object",
    "error": {
        "code": "integer",
        "message": "string",
        "details": [
            "object"
        ]
    },
    "response": "object",

*/