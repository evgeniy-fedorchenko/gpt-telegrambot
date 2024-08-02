package com.evgeniyfedorchenko.gptbot.data;

import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;

import java.util.List;

public interface RedisService {

    List<GptMessageUnit> getHistory(String userChatId);

    void addMessage(String userChatId, GptMessageUnit gptMessageUnit);

    Boolean exist(String userChatId);
}
