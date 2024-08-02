package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageResponse {

    private String image;
    private String modelVersion;
}
