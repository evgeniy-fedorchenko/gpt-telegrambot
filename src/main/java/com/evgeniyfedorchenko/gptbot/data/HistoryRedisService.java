package com.evgeniyfedorchenko.gptbot.data;

import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;

import java.util.List;

public interface HistoryRedisService {

    List<GptMessageUnit> getHistory(String userChatId);

    default List<GptMessageUnit> getHistory(Long userChatId) {
        return this.getHistory(String.valueOf(userChatId));
    }

    void addMessage(String userChatId, GptMessageUnit gptMessageUnit);

    default void addMessage(Long userChatId, GptMessageUnit gptMessageUnit) {
        this.addMessage(String.valueOf(userChatId), gptMessageUnit);
    }

    void clean(String chatId);
}
