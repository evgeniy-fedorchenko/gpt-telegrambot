package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.telegram.Mode;

public interface UserModeRedisService {

    Mode getMode(String userChatId);

    default Mode getMode(Long userChatId) {
        return this.getMode(String.valueOf(userChatId));
    }

    void setMode(String userChatId, Mode mode);

    default void setMode(Long userChatId, Mode mode) {
        this.setMode(String.valueOf(userChatId), mode);
    }
}
