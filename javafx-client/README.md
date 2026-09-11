# JavaFX client — КР2

Здесь находится только каркас будущего самостоятельного desktop-приложения
на Java 21 и JavaFX 21. POM, JavaFX Application, FXML, CSS и зависимости ещё
не созданы; каталог пока не участвует в Maven-сборке.

Архитектура КР2: View/FXML → Controller → Service → Repository/DAO/JDBC →
PostgreSQL. JavaFX подключается к БД напрямую через JDBC и не требует backend КР3.

Пакеты `app`, `controller`, `model`, `repository`, `service`, `exception`,
`config`, `util` находятся под `com.logisticscompany.javafx`. Ресурсы `fxml/`,
`css/`, `config/` и `src/test/` зарезервированы для будущей реализации.

TableView, ObservableList, диалоги, CRUD, поиск, фильтры, сортировка,
статистика, экспорт через FileChooser и валидация — задачи КР2.
См. [архитектуру](../docs/ARCHITECTURE.md) и [план](../docs/ROADMAP.md).
