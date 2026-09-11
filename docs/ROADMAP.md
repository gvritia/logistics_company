# План развития

Текущее положение: **Project foundation / начало КР1**. Структура и документация подготовлены; функциональная реализация всех четырёх КР впереди. Подробный рабочий backlog находится в [TASKS.md](TASKS.md).

## Phase 1 — KR1 Console

**Текущий этап: foundation. Следующая работа — проектирование КР1.**

- Уточнить поля `TransportationRequest` и `Client`, enum статусов и минимум пять бизнес-правил.
- Спроектировать PostgreSQL schema и документировать связи и ограничения.
- Подготовить локальную конфигурацию и JDBC-подключение.
- Реализовать Repository с `PreparedStatement`, затем Service и собственные исключения.
- Добавить консольный UI, CRUD, поиск, фильтрацию, сортировку, статистику и XLSX export.
- Проверить бизнес-правила и доступ к данным, документировать запуск.
- Отдельно оценить безопасный перенос корневого Maven-проекта в `console-app/` с учётом IDE-настроек и повторной проверки сборки.

Результат этапа: работающее консольное приложение со слоями UI → Service → Repository/JDBC → PostgreSQL.

## Phase 2 — KR2 JavaFX

**Запланировано.**

- Создать отдельный Maven-проект JavaFX 21 на Java 21.
- Определить повторное использование правил и модели КР1 без копирования бизнес-логики и преждевременных библиотек.
- Реализовать прямой доступ к PostgreSQL через JDBC.
- Добавить FXML, Controller, CSS, TableView, ObservableList и диалоги.
- Реализовать CRUD, поиск, фильтры, сортировку, статистику, экспорт, FileChooser и валидацию.
- Проверить интерфейс, слои и сценарии работы с БД.

Результат этапа: самостоятельный desktop-клиент, которому не нужен REST backend КР3.

## Phase 3 — KR3 Spring REST Backend

**Запланировано.**

- Создать отдельный Spring Boot backend на Java 21.
- Подготовить переход существующей схемы на Flyway и установить `ddl-auto: validate`.
- Реализовать JPA domain, repositories, services, DTO и REST controllers.
- Добавить Bean Validation, общий error handling, фильтрацию и пагинацию.
- Реализовать users, роли, Spring Security, JWT access + refresh и refresh sessions.
- Добавить notes, tags, audit, bulk operations, analytics и CSV import/export.
- Настроить Swagger/OpenAPI, Actuator, Docker / Docker Compose.
- При необходимости разместить web-интерфейс в `backend/src/main/resources/static/`.
- Проверить API, права доступа, миграции и совместимость схемы.

Результат этапа: документированный REST backend на базе предметной модели и БД предыдущих КР.

## Phase 4 — KR4 Android

**Запланировано.**

- Создать полноценный Android-проект в Android Studio с Gradle.
- Использовать Java и source compatibility 17.
- Подключить Retrofit, OkHttp, Gson и API backend КР3.
- Реализовать интерфейс с RecyclerView и Material Components и необходимые пользовательские сценарии.
- Добавить работу с авторизацией и ошибками API согласно готовому контракту КР3.
- Проверить мобильные сценарии и интеграцию с backend.

Результат этапа: Android-клиент, работающий через REST, без прямого доступа к PostgreSQL.

Переход к следующему этапу выполняется отдельными задачами. Наличие места для будущего компонента не является разрешением реализовывать его заранее.
