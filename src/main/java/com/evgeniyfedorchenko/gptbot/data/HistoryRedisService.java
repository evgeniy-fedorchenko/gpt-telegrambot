package com.evgeniyfedorchenko.gptbot.data;

import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;

import java.util.List;

public interface HistoryRedisService {

    List<GptMessageUnit> getHistory(String userChatId);

    void addMessage(String userChatId, GptMessageUnit gptMessageUnit);

}
