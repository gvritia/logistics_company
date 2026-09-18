package com.lcorp.console.repository.hibernate;

import com.lcorp.console.model.Client;
import com.lcorp.console.repository.ClientRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Реализация существующего интерфейса: ClientService не нужно знать о Hibernate
public class HibernateClientRepository implements ClientRepository {

    private final SessionFactory sessionFactory;

    public HibernateClientRepository(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    public Client create(Client client) {
        Objects.requireNonNull(client);
        if (client.getId() != null) {
            throw new IllegalArgumentException("Для create нужен клиент без ID");
        }
//  inTransaction открывает Session, начинает Transaction, затем
//  выполняет мой код, делает commit и закрывает Session
        return inTransaction(session -> {
            // persist говорит Хибернету, что этот объект является новой сущностью
            // и его нужно сохранить в базе
            session.persist(client);
            return client;
        });
    }

    @Override
    public Optional<Client> findById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session -> Optional.ofNullable(session.find(Client.class, id)));
    }

    @Override
    public List<Client> findAll() {
        // Это HQL: Client — имя Java-класса. SQL к таблице clients строит Hibernate
        // Сортировка по ID делает порядок списка предсказуемым
        return inTransaction(session -> session
            .createSelectionQuery("from Client c order by c.id", Client.class)
            .getResultList());
    }

    @Override
    public boolean update(Client client) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(client.getId());

        return inTransaction(session -> {
            Client existing = session.find(Client.class, client.getId());
            if (existing == null) {
                return false;
            }

            existing.setFullName(client.getFullName());
            existing.setPhone(client.getPhone());
            existing.setEmail(client.getEmail());
            return true;
        });
    }

    @Override
    public boolean deleteById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session -> {
            Client existing = session.find(Client.class, id);
            if (existing == null) {
                return false;
            }
            // Проверка истории остаётся в Service, а FK в БД защищает от потери связей
            session.remove(existing);
            return true;
        });
    }

    // Function описывает действие: получает Session и возвращает результат типа T
    // Общий метод позволяет не повторять открытие, commit и rollback в каждом CRUD
    private <T> T inTransaction(Function<Session, T> action) {
        // Session живёт только в рамках одной операции и всегда закрывается
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = action.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                try {
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                } catch (RuntimeException rollbackException) {
                    // Сохраняем исходную причину, даже если сам откат тоже завершился ошибкой
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }
}
