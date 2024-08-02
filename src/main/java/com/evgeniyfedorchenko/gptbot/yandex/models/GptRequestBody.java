package com.evgeniyfedorchenko.gptbot.yandex.models;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Класс представляет собой тело запроса, который направляется на обработку модели Яндекса
 */
@Getter
@Builder
public class GptRequestBody {

    /**
     * Внутренний адрес модели Яндекса
     * @example {@code "gpt://<your-yandex-folder-id>/yandexgpt-lite}
     */
    private final String modelUri;

    /**
     * Настройки ответа: режим потока, температура, ограничение по токенам
     */
    @Builder.Default
    private final CompletionOptions completionOptions
            = new CompletionOptions(false, 0.5D, 2000);

    /**
     * История сообщений,"контекст"
     */
    private final List<GptMessageUnit> messages;

    /**
     * Технические настройки желаемого ответа
     *
     * @param stream      Режим потока - передавать ответ только когда он будет полностью готов или по частям
     * @param temperature Мера непредсказуемости/креативности. Число от [0,1]
     * @param maxTokens   Лимит токенов на ответ, максимум 2000 токенов
     */
    public record CompletionOptions(boolean stream, double temperature, int maxTokens) { }

}

/*
Request:

curl --request POST
 -H "Content-Type: application/json"
 -H "Authorization: Bearer {your_iam_token}"
 -H "x-folder-id: {your_folder_id}"
 -d '{
  "modelUri": "gpt://{your_folder_id}/yandexgpt-lite",
  "completionOptions": {
    "stream": false,
    "temperature": 0.6,
    "maxTokens": "2000"
  },
  "messages": [
  *Вся история чата. Новые - снизу*
    {
      "role": "user",
      "text": "Привет, это тестовый запрос. Отзовись, если ты слышишь меня"
    }
  ]
}'   "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"

*/
