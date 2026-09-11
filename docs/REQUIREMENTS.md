# Требования по контрольным работам

Документ описывает будущие результаты. На текущем этапе подготовлена только структура; перечисленная функциональность ещё не реализована.

## КР1 Console

- Консольное приложение на Java 21, Maven 3.9+, PostgreSQL и JDBC.
- Основная сущность `TransportationRequest`, связанная сущность `Client`, enum статуса заявки.
- CRUD, поиск, фильтрация, сортировка, статистика и экспорт XLSX.
- Минимум пять согласованных бизнес-правил и собственные исключения.
- Доступ к данным через repository и JDBC; параметризованные запросы через `PreparedStatement`.
- Разделение Console UI, Service и Repository; SQL не находится в UI.

Конкретные поля, правила и условия поиска нужно согласовать перед соответствующей реализацией. Функции пользователей, ролей и JWT не входят в подготовку модели КР1.

## КР2 JavaFX

- Отдельное приложение на Java 21 и JavaFX 21 с FXML, Controller и CSS.
- `TableView`, `ObservableList`, CRUD и диалоги, поиск, фильтры, сортировка, статистика и экспорт.
- `FileChooser` для выбора файла экспорта, валидация пользовательского ввода.
- Слои View/FXML → Controller → Service → Repository/DAO/JDBC → PostgreSQL.
- Прямое подключение к PostgreSQL через JDBC, без зависимости от REST backend КР3.

## КР3 Spring REST

- Отдельный Spring Boot backend на Java 21.
- REST controllers, services, Spring Data JPA repositories, JPA domain и DTO.
- Spring Security, роли, JWT access + refresh, пользователи и refresh sessions.
- Bean Validation, единая обработка ошибок, фильтрация и пагинация.
- Swagger/OpenAPI, Actuator, Docker / Docker Compose.
- CSV import/export, analytics, audit, notes, tags и bulk operations.
- Flyway для изменений схемы PostgreSQL, основанной на схеме КР1.
- Hibernate `ddl-auto: validate`; Hibernate не управляет изменениями схемы.

Если потребуется web-интерфейс, место для него — `backend/src/main/resources/static/`. Отдельный React/Vue/Angular frontend добавляется только по отдельному запросу.

## КР4 Android

- Отдельный Android-проект, создаваемый в Android Studio на этапе КР4.
- Язык Java, Gradle, Java source compatibility 17.
- Retrofit, OkHttp и Gson для взаимодействия с REST API backend КР3.
- RecyclerView и Material Components для интерфейса.
- Работа с backend по HTTP/REST; прямое подключение Android к PostgreSQL запрещено.

## Требования, влияющие на архитектуру

1. Четыре КР — последовательные этапы одного монорепозитория, а не независимые проекты с несвязанными моделями.
2. PostgreSQL и схема КР1 становятся основой дальнейшего развития. Начиная с КР3 изменения оформляются миграциями Flyway.
3. КР1 и КР2 имеют собственные точки запуска и используют JDBC. КР2 не требует работающего Spring backend.
4. Backend КР3 отделён от desktop-клиента; Android КР4 использует этот backend через REST.
5. UI отвечает за представление, Service — за бизнес-правила, Repository/DAO — за хранение и чтение. Не следует дублировать бизнес-правила между интерфейсами.
6. Новый Java-код использует базовый package `com.logisticscompany`; для Android уровень исходников 17, для остальных Java-компонентов — 21.
7. Секреты и локальные данные не хранятся в Git. `.env.example` содержит только безопасные placeholders.
8. Сейчас запрещена реализация перечисленных функций, SQL, миграций, приложений и Docker Compose. Допускаются структура, документация и безопасные шаблоны.
