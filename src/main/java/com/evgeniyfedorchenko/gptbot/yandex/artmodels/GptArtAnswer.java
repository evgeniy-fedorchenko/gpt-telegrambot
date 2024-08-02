package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GptArtAnswer {

    private String id;
    private String description;
    private String createdAt;
    private String createdBy;
    private String modifiedAt;
    private boolean done;
    private String metadata;
    private Error error;
    private ImageResponse response;

}
