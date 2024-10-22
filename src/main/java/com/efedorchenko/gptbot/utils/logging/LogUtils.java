package com.efedorchenko.gptbot.utils.logging;

import com.efedorchenko.gptbot.aop.MethodLogAspect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogUtils {

    //     Markers for use in loggers
    public static final Marker LOGIC_MARKER = MarkerFactory.getMarker("LOGIC");
    public static final Marker POWER_MARKER = MarkerFactory.getMarker("POWER");
    public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker RANRE_MARKER = MarkerFactory.getMarker("RetryAttemptNotReadyException");
    public static final Marker FUTURE_CHECK = MarkerFactory.getMarker("CHECK");

    //     Message's patterns for logging main stages processing update
    public static final String BEGUN = "Processing has BEGUN for updateID {}";
    public static final String FINISHED_NORMALLY = "Processing has FINISHED normally for updateID {}";
    public static final String FINISHED_NORMALLY_NULL = "Processing has FINISHED (null specially, not sent anything) for updateID {}";
    public static final String FILED_UNHANDLED = "Processing has FAILED (NOT HANDLED) for updateID {}. Ex: ";

    //     Patterns for declare logging
    private static final String PARAMS = "-> [{}]";
    private static final String RETURN = "<- [{}]";
    private static final String EX = "!- [{}]";

    //     Spring java beans or other fields
    private static final String BASE64_REGEX = "([A-Za-z0-9+/]{1000,})(=*)";


    private final ObjectMapper objectMapper;

    @Value("${logging.max-mess-length}")
    private int maxLength;

    /**
     * Метод для универсального асинхронного логирования срезов методов
     * <p>
     * Метод логирует входные параметры и результаты методов, срезанных в {@link MethodLogAspect}. Логированию
     * подлежат как входные параметры, так и возвращаемые значения, а так же исключения, если они возникли в ходе
     * работы метода. Детализация логирования настраивается для каждого метода отдельно. См. аннотацию {@link Log}.
     * Метод адаптирован к {@link CompletableFuture} и логирует только результат (или исключение)<br>
     * Метод не адаптирован к оберткам объектов реактивного стека ({@code Mono}, {@code Flux})
     *
     * @param paramsForLogFuture список параметров метода (их значений). Параметры, исключенные из логирования,
     *                           должны быть уже отсеяны. Перед логированием результат будет получен безопасно
     * @param result             значение, возвращенное методом (или экземпляр {@link Throwable}). Возвращаемое значение будет
     *                           залогировано в соответствии с флагом {@link Log#result()}. {@link Throwable} будет залогирован
     *                           в любом случае. Если возвращаемое значение представляет собой экземпляр {@link CompletableFuture},
     *                           то перед логированием он будет безопасно получен
     * @param targetlogger       целевой логгер, с помощью которого будет произведено логирование
     * @param log                настройки, в соответствии с которыми произойдет логирование
     */
    @Async("executorOfVirtual")
    public void doLogMethodAsyncForSomeone(@Nullable CompletableFuture<List<Object>> paramsForLogFuture,
                                           Object result,
                                           Logger targetlogger,
                                           Log log) {

        BiConsumer<String, String> logger = logFunction(targetlogger, log.level());

        if (paramsForLogFuture != null) {
            logger.accept(PARAMS, formatFutures(paramsForLogFuture));
        }
        switch (result) {
            case Throwable throwable -> logger.accept(EX, throwable.toString());
            case CompletableFuture<?> resultFuture -> resultFuture.whenComplete((sucComplete, exComplete) -> {
                if (exComplete != null) {
                    logger.accept(EX, exComplete.toString());
                } else if (log.result()) {
                    logger.accept(RETURN, format(sucComplete));
                }
            });
            default -> {
                if (log.result()) {
                    logger.accept(RETURN, format(result));
                }
            }
        }
    }

    private BiConsumer<String, String> logFunction(Logger logger, Level level) {
        return switch (level) {
            case TRACE -> logger::trace;
            case DEBUG -> logger::debug;
            case WARN -> logger::warn;
            case ERROR -> logger::error;
            default -> logger::info;
        };
    }

    public String format(Object object) {

        try {
            String string = (object instanceof String ? (String) object : objectMapper.writeValueAsString(object))
                    .replaceAll("\\n", "")
                    .replaceAll(BASE64_REGEX, "base64 encoding");

            String result = truncate(objectMapper.readTree(string), 2).toString();
            return result.length() > maxLength
                    ? "..>" + result.substring(result.length() - maxLength)
                    : result;

        } catch (IOException ex) {
            // TODO 08.09.2024 14:49: разобраться почему спотыкается на ссылках
            return "impossible to deserialize <" + object + ">";
        }
    }

    private String formatFutures(CompletableFuture<List<Object>> runningFuture) {
        try {
            List<Object> objects = runningFuture.get();
            String[] strings = new String[objects.size()];

            for (int i = 0; i < objects.size(); i++) {
                strings[i] = format(objects.get(i));
            }
            return String.join(", ", strings);

        } catch (InterruptedException | ExecutionException ex) {
            log.error(LOGIC_MARKER, "Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return "Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage();
        }
    }

    private JsonNode truncate(JsonNode node, int maxElements) {
        return switch (node) {
            case ArrayNode arrayNode when arrayNode.size() > maxElements -> {
                ArrayNode truncatedArray = JsonNodeFactory.instance.arrayNode();
                JsonNode firstElement =
                        JsonNodeFactory.instance.textNode("<.. " + (arrayNode.size() - maxElements) + " hide ..>");
                truncatedArray.add(firstElement);
                for (int i = arrayNode.size() - maxElements; i < arrayNode.size(); i++) {
                    truncatedArray.add(arrayNode.get(i));
                }
                yield truncatedArray;
            }
            case ObjectNode objectNode -> {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                objectNode.fields().forEachRemaining(f -> result.set(f.getKey(), truncate(f.getValue(), maxElements)));
                yield result;
            }
            default -> node;   // or case "ArrayNode arrayNode arrayNode.size() <= maxElements"
        };
    }
}
