# Logistics Company

Учебный проект команды из четырёх разработчиков: информационная система
логистической компании. Основная сущность — `TransportationRequest`
(заявка на перевозку груза), связанная — `Client` (клиент).
Все четыре контрольные работы последовательно развивают одну предметную
область и общую схему PostgreSQL в одном репозитории.

**Текущий статус: Project foundation / начало КР1 / Structure only.**
Подготовлены каталоги, документация и безопасные шаблоны. Реализация КР1
ещё впереди: моделей, CRUD, JDBC, меню и базы данных пока нет.
Существующий пустой класс `org.exaple.Main` сохранён без изменений.

## Этапы

| Этап | Компонент | Доступ к данным |
| --- | --- | --- |
| КР1 | Console, Java 21, Maven | Service → Repository/JDBC → PostgreSQL |
| КР2 | Отдельный JavaFX 21 desktop-клиент | Controller → Service → DAO/JDBC → PostgreSQL |
| КР3 | Spring Boot REST backend, Java 21 | Service → Spring Data JPA → PostgreSQL; Flyway |
| КР4 | Android на Java, source level 17, Gradle | HTTP/REST → backend КР3 |

JavaFX КР2 работает напрямую с PostgreSQL и не зависит от backend КР3.
Android использует только REST API. Отдельного SPA-проекта нет.

## Структура сейчас

```text
logistics_company/
├── AGENTS.md, README.md, CONTRIBUTING.md, CHANGELOG.md
├── .gitignore, .editorconfig, .gitattributes, .env.example
├── pom.xml                         # действующий Maven-проект КР1
├── src/                            # исходники и каркас пакетов КР1
│   ├── main/java/com/logisticscompany/console/
│   ├── main/java/org/exaple/Main.java  # сохранённая исходная заготовка
│   ├── main/resources/
│   └── test/java/
├── docs/                           # контекст, требования, решения и backlog
├── database/{schema,seed,diagrams}/
├── console-app/                    # README, резерв будущего переноса
├── javafx-client/src/              # каркас КР2 без POM и реализации
├── backend/src/                    # каркас КР3 без POM и реализации
├── android-client/                 # только README до КР4
├── infra/{docker,scripts}/
└── .github/                        # PR template; workflows зарезервирован
```

Maven пока остаётся в корне, чтобы сохранить текущие пути проекта и IDE.
Новых Maven-модулей и корневого агрегатора нет. Обоснование и условия
будущего переноса в `console-app/` записаны в [DECISIONS.md](docs/DECISIONS.md).
Новый код должен использовать базовый package `com.logisticscompany`.

## Проверка foundation

Нужны JDK 21 и Maven 3.9+. Из корня репозитория:

```sh
mvn -version
mvn -B test
```

Команда компилирует текущую заготовку и запускает доступные тесты;
тестов пока нет. Запускаемого приложения нет. PostgreSQL потребуется
для будущей реализации КР1, но не для проверки foundation.
`.env.example` содержит placeholders; его автоматическая загрузка пока
не реализована. Рабочие `.env` не должны попадать в Git.

## Документация

Начните с [AGENTS.md](AGENTS.md) и [контекста](docs/PROJECT_CONTEXT.md).
Далее: [требования](docs/REQUIREMENTS.md), [архитектура](docs/ARCHITECTURE.md),
[модель](docs/DOMAIN_MODEL.md), [БД](docs/DATABASE.md),
[дорожная карта](docs/ROADMAP.md), [разработка](docs/DEVELOPMENT.md),
[Git workflow](docs/GIT_WORKFLOW.md), [решения](docs/DECISIONS.md)
и [задачи](docs/TASKS.md).
Правила команды: [CONTRIBUTING.md](CONTRIBUTING.md);
история изменений: [CHANGELOG.md](CHANGELOG.md).
