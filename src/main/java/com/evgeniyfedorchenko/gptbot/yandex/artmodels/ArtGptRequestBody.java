package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ArtGptRequestBody {

    private final String modelUri;

    private final List<ArtMessage> messages;

    private final GenerationOptions generationOptions;

}