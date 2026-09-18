package com.lcorp.console.repository.hibernate;

import com.lcorp.console.model.Driver;
import com.lcorp.console.repository.DriverRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class HibernateDriverRepository implements DriverRepository {

    private final SessionFactory sessionFactory;

    public HibernateDriverRepository(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    public Driver create(Driver driver) {
        Objects.requireNonNull(driver);
        if (driver.getId() != null) {
            throw new IllegalArgumentException("Для create нужен доставщик без ID");
        }
        return inTransaction(session -> {
            session.persist(driver);
            return driver;
        });
    }

    @Override
    public Optional<Driver> findById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session -> Optional.ofNullable(session.find(Driver.class, id)));
    }

    @Override
    public List<Driver> findAll() {
        return inTransaction(session -> session
            .createSelectionQuery("from Driver d order by d.id", Driver.class)
            .getResultList());
    }

    @Override
    public boolean update(Driver driver) {
        Objects.requireNonNull(driver);
        Objects.requireNonNull(driver.getId());
        return inTransaction(session -> {
            Driver existing = session.find(Driver.class, driver.getId());
            if (existing == null) {
                return false;
            }

            // Загруженный объект управляется Hibernate: commit сохранит эти изменения
            existing.setFullName(driver.getFullName());
            existing.setPhone(driver.getPhone());
            existing.setActive(driver.isActive());
            return true;
        });
    }

    @Override
    public boolean deleteById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session -> {
            Driver existing = session.find(Driver.class, id);
            if (existing == null) {
                return false;
            }
            // При наличии заявок внешний ключ в БД запретит удаление
            session.remove(existing);
            return true;
        });
    }

    @Override
    public List<Driver> findActive() {
        // active — имя поля Java-модели. HQL выполняет фильтрацию в БД
        return inTransaction(session -> session
            .createSelectionQuery("from Driver d where d.active = true order by d.id", Driver.class)
            .getResultList());
    }

    private <T> T inTransaction(Function<Session, T> action) {
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
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }
}
