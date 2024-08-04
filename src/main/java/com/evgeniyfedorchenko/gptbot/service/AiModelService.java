package com.evgeniyfedorchenko.gptbot.service;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.Serializable;

public interface AiModelService {

    PartialBotApiMethod<? extends Serializable> newCall(Message inputMess);

//    PartialBotApiMethod<? extends Serializable> getSendObject(<?> modelAnswer);

}
