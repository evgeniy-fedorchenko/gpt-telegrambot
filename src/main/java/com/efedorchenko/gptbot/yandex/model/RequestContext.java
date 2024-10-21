package com.efedorchenko.gptbot.yandex.model;

import com.efedorchenko.gptbot.yandex.service.YandexArtService;
import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;

@Getter
@Setter

public class RequestContext {

    /**
     * Счетчик процентов. Показывает прогресс генерации изображения. На самом деле не имеет связи с процессом
     * генерации, а просто постепенно увеличивается с все замедляющейся скоростью, никогда не достигая {@code 100%},
     * расчет значения происходит в методе {@link YandexArtService#calculatePercentReady(double current)}<br>
     * Значение в начале генерации - {@code 1%}<br>
     * После каждой генерации сбрасывается на {@code 1%}
     */
    private double percentReady = 1;

    /**
     * Сообщение, отправляемое юзеру, для уведомления его о процессе генерации. Отправляемое сообщение - объект
     * {@link EditMessageText}, после отправки возвращающий этот объект. Поле нужно для хранения контекста этого
     * сообщения, включающего {@link Message#getMessageId()}. Необходимо, чтобы держать юзера в курсе о процессе
     * генерации, т.к. это занимает продолжительное время. Как правило, это сообщение содержит текст
     * {@code Генерация завершена на X%}. В качестве счетчика процентов выступает поле
     * {@link RequestContext#percentReady}, который рассчитывается на основе прогресса генерации
     */
    private Message generationProcessMess;

}
