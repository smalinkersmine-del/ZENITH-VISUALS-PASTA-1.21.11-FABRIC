@echo off
chcp 65001 >nul
where gradle >nul 2>nul
if errorlevel 1 (
  echo Не найден Gradle. Установи Gradle 8.14+ и добавь его в PATH.
  echo Также нужен JDK 21.
  pause
  exit /b 1
)
gradle build
if errorlevel 1 (
  echo Сборка завершилась с ошибкой. Проверь сообщения выше.
) else (
  echo Готовый JAR находится в build\libs\
)
pause
