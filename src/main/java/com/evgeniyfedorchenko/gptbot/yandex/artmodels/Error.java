package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Error {

    private int code;
    private String message;
    private List<String> details;
    private final String response;
}
