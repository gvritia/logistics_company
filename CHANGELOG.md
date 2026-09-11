# Changelog

Здесь фиксируются значимые изменения общего проекта КР1–КР4.
Записи описывают фактически выполненную работу; планы находятся в
[ROADMAP.md](docs/ROADMAP.md) и [TASKS.md](docs/TASKS.md).

## Unreleased

### Added

- Каркас пакетов КР1 в существующем `src/` и каталоги будущих компонентов
  JavaFX, backend, Android, database и infra.
- Контекст, требования, архитектура, предварительная предметная модель,
  решения, дорожная карта и backlog четырёх КР.
- Правила работы команды и AI-агентов, шаблон Pull Request.
- Настройки EditorConfig, Git attributes, расширенный `.gitignore`
  и `.env.example` с placeholders.

### Preserved

- Корневой Maven-проект Java 21, существующий пустой `org.exaple.Main`
  и настройки IDE. Перенос в `console-app/` отложен и задокументирован.

Фаза: Project foundation. Бизнес-логика, SQL, UI, backend и Android
на этом этапе не реализованы.
