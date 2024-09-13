package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.yandex.service.YandexArtService;
import com.efedorchenko.gptbot.yandex.service.YandexGptService;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.ActionType;

@Getter
@AllArgsConstructor
public enum Mode {

    YANDEX_GPT(YandexGptService.SERVICE_NAME, TelegramDistributor.Command.YA_GPT.getRepresentation(), ActionType.TYPING),
    YANDEX_ART(YandexArtService.SERVICE_NAME, TelegramDistributor.Command.YA_ART.getRepresentation(), ActionType.UPLOADPHOTO),
    YANDEX_ART_HOLD(null, null, null);

    @Nullable
    private final String serviceName;
    @Nullable
    private final String enablingCommand;
    @Nullable
    private final ActionType actionType;

}
