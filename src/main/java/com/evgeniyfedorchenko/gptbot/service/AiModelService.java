package com.evgeniyfedorchenko.gptbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Общий интерфейс для взаимодействия с нейростями
 * <p>
 * Представляем базовые методы жизненного цикла запроса,
 * от подготовки тела до создания объекта, который может быть отправлен через Телеграм. Реализации этого
 * интерфейса включают в себя только логику, необходимую для успешного взаимодействия с целевой нейросетью
 *
 * @param <REQ>  класс тела запроса, посылаемого к модели на генерацию контента
 * @param <RESP> класс, представляющий ответ нейросети в том виде, в котором она его отдает
 */
public interface AiModelService<REQ, RESP> {

    /**
     * Метод для подготовки объекта, который будет десериализован и помещен в тело http-запроса. Этот запрос
     * будет отправлен в нейросеть для генерации контента. Так же в этом методе будут выполнены все специфичные
     * операции связанные с началом обработки сообщения от пользователя в Телеграм - такие, как открытие ресурсов,
     * проверка кешей и т.д.
     *
     * @param inputMess исходный параметр, получаемый от Телеграм. Это все данные предоставляемые вызывающим
     *                  классом для построения тела http-запроса
     * @return готовый объект, который будет десериализован и отправлен в теле http-запроса
     */
    REQ prepareRequest(Message inputMess);

    /**
     * Метод для непосредственного конструирования http-запроса типа POST, отправки его в сеть, и
     * предоставления полученного ответа. Этот метод никак не изменяет тело запроса и ответа, кроме
     * того, что сериализует запрос и десериазизует ответ. Этот метод возвращает тело ответа, даже если
     * запрос завершился со статусом, отличным от {@link HttpStatus#OK}
     *
     * @param url          полный урл (включая параметры пути и параметры запроса) по которому будет отправлен запрос
     * @param requestBody  готовое тело запроса, должно поддерживать сериализацию
     * @param responseType класс, в объект которого будет десериазизован полученный ответ
     * @return {@link Optional}, содержащий тело ответа, десериализованное в экземпляр класса параметра
     * {@code responseType}, полученное с сервера целевой нейросети. Возвращается {@link Optional#empty()}
     * только в случае, если http-ответ не содержит тела
     * @throws JsonProcessingException не удалось десериализовать тело ответа в экземпляр класса {@code responseType}
     * @throws IOException             не удалось выполнить http-запрос, например из-за отмены,
     *                                 проблемы с подключением или тайм-аута
     */
    Optional<RESP> buildAndExecutePost(String url, Object requestBody, Class<RESP> responseType) throws IOException;

    PartialBotApiMethod<? extends Serializable> responseProcess(RESP response, Message sourceMess);

    String getModelUrl();

    Class<RESP> getResponseType();

    // TODO 10.08.2024 22:59: дописать документацию к AiModelService

}
