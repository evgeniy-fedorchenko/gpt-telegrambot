package com.efedorchenko.gptbot.aop;

import com.efedorchenko.gptbot.telegram.TelegramUpdateHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MdcConfigurer {

    public static final String RQUID = "RqUID";
    public static final String MDC_USER = "MdcUser";

    @Pointcut("execution(* com.efedorchenko.gptbot.telegram.TelegramUpdateHandler.handleUpdate(..))")
    public void aroundHandleUpdate() {
    }

    /**
     * Метод для конфигурирования {@link MDC}
     * <p>
     * Контекст устанавливается в начале обратки запроса. Перед вызовом метода-точки входа в приложение
     * ({@link TelegramUpdateHandler#handleUpdate(Update)}) запрос перехватывается этим методом для
     * установки контекста. Как правило, содержит два значения, но гарантируется только одно из них:
     * <lu>
     * <li>{@value RQUID} - уникальный идентификатор запроса. Устанавливается в любом случае.
     * Состоит из 14ти первых символов рандомного UUID (генерируется под каждый запрос) + {@code chatId}
     * юзера, совершившего запрос. Например: {@code 2f2da4aa-0908-5076421775}, где {@code 5076421775}
     * - это {@code chatId} юзера. {@code ChatId} определяется Телеграмом. Если не удалось установить юзера,
     * этот параметр будет содержать полноценный {@code UUID}, полученный как {@code UUID.randomUUID()}</li>
     * <li>{@value MDC_USER} - строковое представление объекта {@link MdcUser} - успешно установленного
     * юзера, от аккаунта которого происходит запрос. Если юзера установить не удалось, этот параметр
     * будет отсутствовать</li>
     * </lu>
     * После установки контекста управление возвращается в перехваченный метод для обработки запроса.
     * После завершения целевого метода, {@link MDC} полностью очищается. Это происходит в любом случае
     *
     * @throws Throwable любое исключение, возникшее в перехваченном методе будет передано выше по стеку
     * @see MdcUser
     * @see MdcUser#toString()
     * @see TelegramUpdateHandler#handleUpdate(Update)
     */
    @Around("aroundHandleUpdate()")
    public Object mdcConfigure(ProceedingJoinPoint joinPoint) throws Throwable {

        if (joinPoint.getArgs()[0] instanceof Update update) {
            MdcUser user = extractUser(update);

            String randomUuid = UUID.randomUUID().toString();
            if (user != null) {
                MDC.put(RQUID, randomUuid.substring(0, 14) + user.getId());
                MDC.put(MDC_USER, user.toString());

            } else {
                log.warn("User for MDC not detected. Use random UUID instead. Update {}", update);
                MDC.put(RQUID, randomUuid);
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            MDC.clear();
        }
    }

    @Nullable
    public MdcUser extractUser(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            User user = update.getMessage().getFrom();
            return MdcUser.builder()
                    .id(String.valueOf(user.getId()))
                    .firstname(user.getFirstName())
                    .lastname(user.getLastName())
                    .username(user.getUserName())
                    .build();

        } else if (update.hasMessage() && update.getMessage().getChat() != null) {
            Chat chat = update.getMessage().getChat();
            return MdcUser.builder()
                    .id(String.valueOf(chat.getId()))
                    .firstname(chat.getFirstName())
                    .lastname(chat.getLastName())
                    .username(chat.getUserName())
                    .build();

        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            User user = update.getCallbackQuery().getFrom();
            return MdcUser.builder()
                    .id(String.valueOf(user.getId()))
                    .firstname(user.getFirstName())
                    .lastname(user.getLastName())
                    .username(user.getUserName())
                    .build();

        } else {
            return null;
        }
    }

    /**
     * Класс представляет объект - пользователя телеграм, от которого исходит запрос. Маппаится из полученного
     * на входе в приложение объекта {@link Update}. Содержит основные поля, идентифицирующие юзера:
     */
    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    static final class MdcUser {

        /** Уникальный (в пределах Телеграм) идентификатор чата с юзером */
        @NotNull
        private final String id;

        /** Имя юзера, устанавливается самим юзером. Может отсутствовать или быть невалидным именем */
        @Nullable
        private final String firstname;

        /** Фамилия юзера, устанавливается самим юзером. Может отсутствовать или быть невалидной фамилией */
        @Nullable
        private final String lastname;

        /** Уникальный (в пределах телеграм) строковый идентификатор юзера. Является ссылкой на диалог с юзером */
        @NotNull
        private final String username;

        @Override
        public String toString() {   // valid json
            return "{\n\"id\": %s,\n\"firstName\": \"%s\",\n\"lastName\": \"%s\",\n\"userName\": \"%s\"\n}"
                    .formatted(id, firstname, lastname, username);
        }

        /**
         * Метод для быстрого преобразования строки в объект {@link MdcConfigurer.MdcUser}
         * <p>
         * Исходная строка должна быть в формате {@code key: value}. Пары ключей-значений
         * перечисляются через запятую. Переносы строк, фигурные скобки и кавычки - опционально.
         *
         * @param src исходная строка, которая будет преобразована в объект {@link MdcConfigurer.MdcUser}
         * @return сформированный POJO или {@code null}, если не удалось собрать объект из предоставленной строки
         */
        @Nullable
        public static MdcUser fromString(String src) { // So much faster than as ObjectMapper
            try {
                String[] split = src.replaceAll("\\s|\\n|\\r|\\{|}|\"", "").split(",");
                return MdcUser.builder()
                        .id(split[0].split(":")[1])
                        .firstname(split[1].split(":")[1])
                        .lastname(split[2].split(":")[1])
                        .username(split[3].split(":")[1])
                        .build();
            } catch (RuntimeException ex) {   // Invalid input parameter
                return null;
            }
        }
    }

}
