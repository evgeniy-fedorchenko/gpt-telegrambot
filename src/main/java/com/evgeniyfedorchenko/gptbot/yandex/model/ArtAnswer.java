package com.evgeniyfedorchenko.gptbot.yandex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

    /**
 * Класс, представляющий ответ модели {@code YandexART}, который она предоставляет СРАЗУ после получения запроса,
 * то есть в нем не содержится сгенерированное изображение. Так как модель генерирует изображение только асинхронно,
 * суть этого класса - вернуть идентификатор (поле {@code id}), по которому юзер сможет опрашивать модель на
 * готовность своего изображения<br>
 * По окончании операции (т.е. когда {@code this.isDone == true}), результат включает только одно
 * из полей {@link Error} или {@link ArtCompleteResponse}
 *
 * @param id          Идентификатор запущенного процесса по генерации изображения.
 * @param description Описание этого процесса генерации. Длина 0–256 символов.
 * @param createdAt   Временная метка начала генерации, в текстовом представлении. Формат RFC3339
 * @param createdBy   Идентификатор пользователя или учетной записи службы, инициировавшей процесс генерации
 * @param modifiedAt  Временная метка последнего изменения этого объекта моделью в процессе генерации. Формат RFC3339
 * @param done        Флаг готовности изображения
 * @param metadata    Метаданные, относящиеся к службе, связанные с этим процессом генерации
 * @param response    Нормальный ответ от модели в случае успеха генерации. Содержит готовое изображение
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtAnswer(@NotNull String id,
                        @NotNull String description,
                        @Nullable String createdAt,
                        @NotNull String createdBy,
                        @Nullable String modifiedAt,
                        boolean done,
                        @Nullable String metadata,
                        @Nullable Error error,
                        @Nullable ArtCompleteResponse response) {

    /**
     * Объект ошибки, содержащий сведения, поясняющие, что именно пошло не так.
     * При наличии поля этого класса в объекте {@link ArtAnswer} поле {@link ArtAnswer#response}
     * недоступно (равно {@code null})
     *
     * @param code    Код ошибки. Значение перечисления <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     * @param message Краткое пояснение к ошибке, подробнее см. <a href="https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto">google.rpc.Code</a>
     * @param details Список сообщений, содержащих сведения об ошибке
     */
    public record Error(int code, @NotBlank String message, @NotNull List<String> details) { }

    /**
     * Объект с результатом успешного процесса генерации, содержащий результат, закодированный в {@code Base64}
     *
     * @param image        Сам результат операции - изображение в кодировке {@code Base64}
     * @param modelVersion Номер модели, сгенерировавшей это изображение. todo проверить
     */
    public record ArtCompleteResponse(String image, String modelVersion) { }
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