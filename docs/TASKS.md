# Backlog

Текущий этап — Project foundation / начало КР1. Ни один пункт реализации
ниже не считается выполненным наличием каталога или README. Одна задача —
одна ветка и небольшой PR. Этапы КР2–КР4 не выполняются заранее.

## Foundation

- [x] Изучить исходный Git и Maven-проект, сохранить код и настройки.
- [x] Подготовить каркас монорепозитория, документацию, правила команды,
  PR template и безопасные конфигурационные шаблоны.
- [ ] Отдельно перенести корневые `pom.xml` и `src/` в `console-app/`
  через `git mv`, согласовав пути существующих IDE-настроек; проверить Maven
  из нового каталога и обновить команды в документации. Решить, нужен ли
  корневой агрегатор, исходя из реально существующих модулей.
- [ ] При начале КР1 отдельно согласовать Maven coordinates и судьбу пустого
  `org.exaple.Main`; весь новый код писать под `com.logisticscompany`.

## КР1 — Console

- [ ] KR1 domain model: согласовать минимальные поля `TransportationRequest`
  и `Client`, предварительный статус заявки и отношения.
- [ ] Зафиксировать минимум пять бизнес-правил, допустимые переходы статусов
  и собственные исключения до реализации сервисов.
- [ ] KR1 PostgreSQL schema: согласовать версию PostgreSQL, ограничения
  и SQL-скрипты в `database/schema/`.
- [ ] Подготовить учебные seed data и ER-диаграмму в `database/`.
- [ ] KR1 JDBC connection: внешняя конфигурация и управление ресурсами.
- [ ] KR1 repositories: CRUD через JDBC и `PreparedStatement`.
- [ ] KR1 services: бизнес-правила, валидация и обработка исключений.
- [ ] KR1 console UI: меню, ввод, вывод и понятные сообщения об ошибках.
- [ ] Поиск, фильтрация и сортировка заявок.
- [ ] Статистика и экспорт XLSX.
- [ ] Проверки бизнес-правил, интеграционные JDBC-проверки и инструкция запуска.

## КР2 — JavaFX

- [ ] Создать отдельный Maven-проект Java 21 / JavaFX 21.
- [ ] Согласовать повторное использование правил КР1 без копирования логики;
  не выделять общий модуль до подтверждения необходимости.
- [ ] Настроить прямой JDBC-доступ к PostgreSQL без зависимости от REST КР3.
- [ ] Создать JavaFX Application, FXML, controllers, CSS и валидацию.
- [ ] TableView / ObservableList, CRUD и диалоги.
- [ ] Поиск, фильтры, сортировка и статистика.
- [ ] Экспорт с FileChooser; проверки и инструкция запуска desktop-приложения.

## КР3 — Spring REST backend

- [ ] Создать отдельный Spring Boot Maven-проект на Java 21.
- [ ] Спланировать перенос схемы КР1 под Flyway: первый baseline/миграция,
  сценарии существующей и новой БД без потери данных.
- [ ] Настроить JPA и `ddl-auto: validate`; последующие изменения — Flyway.
- [ ] Реализовать domain, repositories, services, DTO и REST controllers.
- [ ] Bean Validation, единый error handling, фильтрация и пагинация.
- [ ] Spring Security, роли, users, JWT access + refresh и refresh sessions.
- [ ] Notes/comments, tags, audit и bulk operations.
- [ ] CSV import/export и analytics.
- [ ] Swagger/OpenAPI, Actuator и проверки API.
- [ ] Docker / Docker Compose и документация запуска.
- [ ] При необходимости web-интерфейс в `backend/src/main/resources/static/`.

## КР4 — Android

- [ ] Создать полноценный проект в Android Studio, выбрать SDK/AGP/Gradle.
- [ ] Настроить Java source compatibility 17.
- [ ] Реализовать HTTP-клиент backend КР3: Retrofit, OkHttp, Gson.
- [ ] Авторизация и обновление сессии по контракту REST API.
- [ ] RecyclerView, Material Components и экраны работы с заявками.
- [ ] Клиентская валидация и обработка ошибок сети/API; никаких подключений
  к PostgreSQL.
- [ ] Проверки клиента и инструкция сборки/запуска.

При старте каждой задачи сверяйтесь с [REQUIREMENTS.md](REQUIREMENTS.md),
[ROADMAP.md](ROADMAP.md) и [DECISIONS.md](DECISIONS.md). Уточняемые поля,
переходы статусов и контракты API сначала обсуждаются и фиксируются в docs.
