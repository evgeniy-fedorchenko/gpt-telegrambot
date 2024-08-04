package com.evgeniyfedorchenko.gptbot.data;

import com.evgeniyfedorchenko.gptbot.telegram.Mode;

public interface UserModeRedisService {

    Mode getMode(String userChatId);

    void setMode(String userChatId, Mode mode);
}
