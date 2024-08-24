#!/bin/bash

#------------------------------------------------------------
# Алгоритм скрипта для автоматического и безопасного деплоя:
#
# 0. Запрос суперпользователя
# 1. Установка PATHs для maven и java
# 2. Сборка нового jar-файла с помощью Maven
#
# 3. Переход в папку RUNNING
# 4. Поиск запущенного приложения по PID
# 5. Плавная остановка приложения
# 6. Проверка существования папки archive (и ее создание, при отсутствии)
#
# 7. Перемещение старого jar-файла и каталога env в архивный каталог
# 8. Перемещение нового jar-файла и его каталога env в каталог RUNNING
#
# 10. Запуск нового jar-файла на профиле prod
# 11. Если не запустилось:
#     11.1 Удаляем новый jar-файл и его каталог env
#     11.2 Достаем обратно старый jar-файл и его каталог env
#     11.3 Запускам старый jar-файл и его каталог env
#------------------------------------------------------------


#    Запрос суперпользователя
if [ "$(id -u)" -ne 0 ]; then
  exec sudo -E bash "$0" "$@"
fi


PROJECT_DIR=~/Documents/JavaProjects/gpt-telegrambot          # Путь к каталогу с исходниками Maven проекта
RUNNING_DIR=~/Documents/JavaProjects/gpt-telegrambot_RUNNING  # Путь к каталогу, где находится запущенное приложение
ARCHIVE_DIR=/archive                                          # Путь к каталогу для хранения остановленного приложения
STARTUP_TIMEOUT=10                                            # Время в секундах, через которое будет проверен запуск
JAR_PATTERN="gpt-telegrambot-*.jar"                           # Паттерн JAR файла
OLD_JAR_NAME=""                                               # Название JAR файла, который будет ОСТАНОВЛЕН
NEW_JAR_NAME=""                                               # Название JAR файла, который будет ЗАПУЩЕН


print_message() {   # Функция для выделения сообщений
    echo ""
    echo "===================================================================="
    echo "$1"
    echo "===================================================================="
    echo ""
}


#    Установка PATHs для java и maven
cd "$PROJECT_DIR/target" || { echo "Error at \"cd \$PROJECT_DIR/target\""; exit 1;}
export PATH=$MAVEN_HOME/bin:$PATH
export PATH=$JAVA_HOME/bin:$PATH


#    Сборка с помощью Maven
print_message "Maven packaging"
cd ..
mvn clean package
if [ $? -ne 0 ]; then
  print_message "Maven packaging filed"
  exit 1
fi
print_message "Maven packaging complete"
NEW_JAR_NAME=$(find $JAR_PATTERN 2>/dev/null)




cd "$RUNNING_DIR" || { echo "Error at \"cd \$RUNNING_DIR\""; exit 1;}
OLD_JAR_NAME=$(ls $JAR_PATTERN 2>/dev/null | head -n 1)
# Поиск и остановка работающего приложения
PID=$(pgrep -f "$OLD_JAR_NAME")
if [ -z "$PID" ]; then
    read -r -p "Running application was not found, do you want to continue? [Y/n] " response
    if [ "$response" != "Y" ]; then
        echo "You entered: $response. Deployment canceled"
        exit 0
    fi
else
    echo "Running application detected, PID $PID, send SIGTERM..."
    kill -TERM "$PID"
    while kill -0 "$PID" 2>/dev/null; do
        echo "Waiting for the $OLD_JAR_NAME to stop ($PID)..."
        sleep 3
    done
    print_message "$OLD_JAR_NAME (PID $PID) was stopped"
fi


#    Замена старых jar и env (если они были найдены) на новые. Старые идут в $ARCHIVE_DIR
if [ -f "$OLD_JAR_NAME" ]; then
    if [ -d "$ARCHIVE_DIR" ]; then
        mkdir "$ARCHIVE_DIR"
    fi
    mv -v "$OLD_JAR_NAME" "$ARCHIVE_DIR/"
    mv -v env "$ARCHIVE_DIR/"
fi
cp -r -v "$PROJECT_DIR/target/$NEW_JAR_NAME" "$RUNNING_DIR"
cp -r -v "$PROJECT_DIR/env" "$RUNNING_DIR"


#    Запуск
print_message "Deploying $NEW_JAR_NAME"
START_TIMESTAMP=$(date +%s)
java -jar "$NEW_JAR_NAME" --spring.profiles.active=prod
FINISH_TIMESTAMP=$(date +%s)


#    Проверка и откат по необходимости
duration=$((FINISH_TIMESTAMP - START_TIMESTAMP))
if [ "$duration" -lt "$STARTUP_TIMEOUT" ]; then
    print_message "Deploying $NEW_JAR_NAME FILED. Rollback to $OLD_JAR_NAME"
    rm -v "$NEW_JAR_NAME"
    rm -r -v env
    cp -r -v "$ARCHIVE_DIR/$OLD_JAR_NAME" "$RUNNING_DIR"
    cp -r -v "$ARCHIVE_DIR/env" "$RUNNING_DIR"

    java -jar "$OLD_JAR_NAME" --spring.profiles.active=prod
fi
