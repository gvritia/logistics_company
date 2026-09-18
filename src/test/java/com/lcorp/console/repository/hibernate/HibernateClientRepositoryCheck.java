package com.lcorp.console.repository.hibernate;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.HibernateConfig;
import com.lcorp.console.model.Client;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import java.util.UUID;

// Ручная интеграционная проверка: запускается отдельно, а не при каждом mvn test
// Нужна локальная БД из .env. Меняется только созданный здесь временный клиент
public final class HibernateClientRepositoryCheck {

    public static void main(String[] args) {
        try (SessionFactory factory = HibernateConfig.createSessionFactory(DatabaseConfig.load())) {
            HibernateClientRepository repository = new HibernateClientRepository(factory);
            Client client = new Client("ORM check " + UUID.randomUUID(), null, "check@example.com");

            try {
                repository.create(client);
                require(client.getId() != null, "БД должна выдать ID");
                Long id = client.getId();
                require(repository.findById(id).orElseThrow().getEmail().equals("check@example.com"),
                    "Созданный клиент должен читаться в новой сессии");
                require(repository.findAll().stream().anyMatch(row -> id.equals(row.getId())),
                    "findAll должен содержать нового клиента");

                client.setFullName("Updated ORM check");
                client.setPhone("+7-000-000-00-00");
                client.setEmail(null);
                require(repository.update(client), "Обновление существующей записи");
                Client updated = repository.findById(id).orElseThrow();
                require(updated.getFullName().equals(client.getFullName())
                    && updated.getPhone().equals(client.getPhone()) && updated.getEmail() == null,
                    "Изменения должны сохраниться после commit");

                // Намеренно нарушаем CHECK: транзакция должна откатиться целиком
                client.setFullName(" ");
                try {
                    repository.update(client);
                    throw new AssertionError("БД должна отклонить пустое имя");
                } catch (ConstraintViolationException expected) {
                    require("23514".equals(expected.getSQLState()), "Ожидается ошибка CHECK");
                }
                require(repository.findById(id).orElseThrow().getFullName().equals("Updated ORM check"),
                    "После ошибки должно остаться прежнее имя");

                // Проверяем также чтение остальных моделей, включая enum, даты и BigDecimal
                try (var session = factory.openSession()) {
                    var transaction = session.beginTransaction();
                    session.createSelectionQuery("from Driver", Driver.class).getResultList();
                    session.createSelectionQuery("from Operator", Operator.class).getResultList();
                    session.createSelectionQuery("from TransportationRequest", TransportationRequest.class)
                        .getResultList();
                    transaction.commit();
                }

                require(repository.deleteById(id), "Удаление созданного клиента");
                require(repository.findById(id).isEmpty(), "Удалённая запись не должна находиться");
                require(!repository.deleteById(id), "Повторное удаление возвращает false");
                client.setFullName("Missing client");
                require(!repository.update(client), "update не должен заново создавать удалённую запись");
                require(repository.findById(id).isEmpty(), "update отсутствующей записи не вставляет строку");
            } finally {
                // Убираем свою запись даже при провале проверки; seed-данные не затрагиваем
                if (client.getId() != null) {
                    repository.deleteById(client.getId());
                }
            }
        }
        System.out.println("PASS: schema validation, entity reads, client CRUD, rollback, missing IDs");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
