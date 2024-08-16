#!/bin/bash

#------------------------------------------------------------
# Алгоритм скрипта для автоматического безопасного деплоя:
#
# 0. Определение переменных и функций
# 1. Сборка нового jar-файла с помощью Maven
#
# 2. Переход в папку RUNNING
# 3. Поиск запущенного приложения по PID
# 4. Плавная остановка приложения
# 5. Проверка существования папки last (и ее создание, при отсутствии)
#
# 6. Перемещение старого jar-файла в папку last
# 7. Перемещение старого файла application-prod.properties в папку last
#
# 8. Перемещение нового jar-файла в папку RUNNING
# 9. Перемещение файла application-prod.properties
#
# 10. Запуск нового jar-файла на профиле prod
# 11. Если не запустилось:
#     11.1 Удаляем новый jar-файл и его application-prod.properties
#     11.2 Достаем обратно старый jar-файл и его application-prod.properties
#     11.3 Запускам старый jar-файл и его application-prod.properties
#------------------------------------------------------------


PROJECT_DIR=~/Documents/JavaProjects/gpt-telegrambot              # Путь к директории с исходниками Maven проекта
RUNNING_DIR=~/Documents/gpt-telegrambot/gpt-telegrambot_RUNNING   # Путь к директории, где находится запущенное приложение
JAR_NAME="gpt-telegrambot-0.0.1-SNAPSHOT.jar"                     # Название JAR файла, который будет собран
CONFIG_FILE="application-prod.properties"                         # Название файла конфигурации

function print_message() {   # Функция для выделения сообщений
    echo ""
    echo "===================================================================="
    echo "$1"
    echo "===================================================================="
    echo ""
}

#    Сборка Maven
print_message "Сборка проекта с помощью Maven..."
cd "$PROJECT_DIR" || exit 1
mvn clean package
if [ $? -ne 0 ]; then
  print_message "Ошибка сборки проекта"
  exit 1
fi
print_message "Сборка успешна."

# Перемещение старого jar в папку last
if [ ! -d "$LAST_DIR" ]; then
    mkdir -p "$LAST_DIR"
fi

echo "Перемещение остановленного jar в папку last..."
mv "$JAR_NAME" "$LAST_DIR/"

















