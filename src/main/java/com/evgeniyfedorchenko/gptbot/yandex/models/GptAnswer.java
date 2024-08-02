package com.evgeniyfedorchenko.gptbot.yandex.models;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GptAnswer(@NotNull GptAnswer.Result result) {

    public record Result(List<Alternative> alternatives, Usage usage, String modelVersion) { }

    public record Alternative(GptMessageUnit message, String status) { }

    public record Usage(int inputTextTokens, int completionTokens, int totalTokens) { }

}

/*
Response:

{
  "result": {
    "alternatives": [
      {
        "message": {
          "role": "assistant",
          "text": "Ответ модели здесь"
        },
        "status": "ALTERNATIVE_STATUS_FINAL"
      }
    ],
    "usage": {
      "inputTextTokens": "29",
      "completionTokens": "12",
      "totalTokens": "41"
    },
    "modelVersion": "18.01.2024"
  }
}
*/
