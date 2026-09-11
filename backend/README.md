# Spring REST backend — КР3

Каталог содержит только каркас будущего backend на Java 21, Maven,
Spring Boot, Spring Data JPA и Spring Security. Приложения, POM, зависимостей,
REST endpoints и конфигурации запуска пока нет.

Будущая архитектура: Clients → REST Controller → Service → Spring Data JPA →
PostgreSQL. Пакеты под `com.logisticscompany.backend` обозначают границы
контроллеров, сервисов, репозиториев, JPA domain, DTO, security, filter,
error handling и config.

Схема КР1 станет основой backend. Начиная с КР3 изменения схемы выполняются
Flyway в `src/main/resources/db/migration/`, Hibernate использует
`ddl-auto: validate`. Сейчас миграций и SQL нет.

`src/main/resources/static/` зарезервирован для возможного web-интерфейса КР3.
Отдельный SPA не создаётся. JWT access/refresh, роли, users, notes, tags,
audit, bulk operations, CSV, analytics, OpenAPI, Actuator и Docker Compose
отложены до КР3; см. [требования](../docs/REQUIREMENTS.md).
