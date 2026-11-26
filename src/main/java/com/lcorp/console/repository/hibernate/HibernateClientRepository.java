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


        return inTransaction(session -> {


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

            session.remove(existing);
            return true;
        });
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
