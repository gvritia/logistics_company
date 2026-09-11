# Android client — КР4

Место для будущего Android-клиента backend КР3. На этапе foundation здесь
только этот README. Полный проект будет создан в Android Studio при начале
КР4; сейчас нет Gradle-файлов, wrapper, Activities или UI.

Язык приложения — Java; Java source compatibility — 17. Сборка — Gradle.
Версии Android SDK и Android Gradle Plugin будут выбраны при создании проекта.
Планируются Retrofit, OkHttp, Gson, RecyclerView и Material Components.

Архитектура: Android → HTTP/REST → Spring Boot backend → PostgreSQL.
Android никогда не подключается напрямую к PostgreSQL и не содержит JDBC
или учётных данных БД. Проверки на клиенте не заменяют бизнес-правила backend.

См. [архитектуру](../docs/ARCHITECTURE.md) и [этап КР4](../docs/ROADMAP.md).
