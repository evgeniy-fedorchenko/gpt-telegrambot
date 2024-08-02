package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtMessage {

    @Builder.Default
    private final double weight = 1D;

    private final String text;

}
