# Контекст проекта

```text
Project: Logistics Company
Current phase: Project foundation
Current control work: KR1
Implementation status: Structure only
```

Logistics Company — учебный командный проект четырёх разработчиков в предметной области «Логистическая компания». Все четыре контрольные работы развивают одну систему, общую предметную модель и базу данных.

Основная сущность — `TransportationRequest` («Заявка на перевозку груза»), связанная сущность — `Client` («Клиент»). Будущая система предназначена для учёта заявок и клиентов, сопровождения перевозок, поиска, анализа и экспорта данных.

## Текущее состояние

Подготовлен только фундамент: документация, правила совместной работы, конфигурационные шаблоны и места для будущего кода. Реализация КР1 ещё впереди. Существующий Maven-проект остаётся в корне; его исходный класс `org.exaple.Main` сохранён без изменений. Каталог `console-app/` обозначает планируемый перенос, но пока не является Maven-модулем.

В `javafx-client/` и `backend/` находятся только заготовки структуры. `android-client/` содержит описание будущего приложения, без Gradle-проекта. Отсутствуют реализованные сущности, CRUD, подключение к БД, интерфейсы и API.

## Технологии по этапам

| Этап | Будущий компонент | Технологии и доступ к данным |
| --- | --- | --- |
| КР1 | Консольное приложение | Java 21, Maven 3.9+, JDBC, PostgreSQL, экспорт XLSX |
| КР2 | Отдельный desktop-клиент | Java 21, Maven, JavaFX 21, FXML, JDBC напрямую к PostgreSQL |
| КР3 | Отдельный backend | Java 21, Maven, Spring Boot, Spring Data JPA, Spring Security, REST, JWT, Flyway, Docker / Docker Compose |
| КР4 | Android-клиент | Java с source compatibility 17, Gradle, Retrofit, OkHttp, Gson, RecyclerView, Material Components, REST backend КР3 |

Целевой базовый package нового кода — `com.logisticscompany`. Старый package и Maven-координаты будут изменены отдельной задачей, если это потребуется; название университета в package не используется.

## Как продолжать проект

Перед каждой задачей прочитать документы в порядке из [AGENTS.md](../AGENTS.md), определить её контрольную работу и границы. Следующая работа относится к началу КР1; функции последующих КР не реализуются заранее. Требования собраны в [REQUIREMENTS.md](REQUIREMENTS.md), архитектура — в [ARCHITECTURE.md](ARCHITECTURE.md), принятые решения — в [DECISIONS.md](DECISIONS.md), backlog — в [TASKS.md](TASKS.md).
