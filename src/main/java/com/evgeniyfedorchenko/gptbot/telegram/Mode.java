package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.yandex.service.YandexArtService;
import com.evgeniyfedorchenko.gptbot.yandex.service.YandexGptService;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mode {

    YANDEX_GPT(YandexGptService.SERVICE_NAME, TelegramDistributor.Command.YA_GPT.getRepresentation()),
    YANDEX_ART(YandexArtService.SERVICE_NAME, TelegramDistributor.Command.YA_ART.getRepresentation()),
    YANDEX_ART_HOLDED(null, null);

    private final String serviceName;
    private final String enablingCommand;

    public static Mode ofCommand(final String command) {
        for (Mode mode : Mode.values()) {
            if (mode.getEnablingCommand() != null && mode.getEnablingCommand().equals(command)) {
                return mode;
            }
        }
        return null;
    }
}
