FROM eclipse-temurin:21-jdk-jammy
LABEL authors="evgeniy-fedorchenko"

WORKDIR /app
COPY target/gpt-telegrambot-0.0.1-SNAPSHOT.jar /app/gptbot001.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/gptbot001.jar"]
#CMD ["bash"]

# Собрать образ: docker build -t app-img -f ./Dockerfile .
# Запустить: docker run -it --rm my-img
# Опции: -it (запустить терминал сразу),
#        -rm (удалить контейнер после установки),
#        -f (указать путь к Dockerfile)
#        exit (выйти из терминала контейнера)
#        docker stop <container-id> (остановить контейнер)
#        docker ps (список контейнеров, доступно с хоста)
#        docker cp <container_id>:<path_in_container> <path_on_host> (скопировать файлы из контейнера на хост)
#        docker cp <path_on_host> <container_id>:<path_in_container> (скопировать файлы с хоста в контейнер
#