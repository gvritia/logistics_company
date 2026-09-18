package com.lcorp.console.repository.hibernate;

import com.lcorp.console.model.Operator;
import com.lcorp.console.repository.OperatorRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;


public class HibernateOperatorRepository implements OperatorRepository {
    private final SessionFactory sessionFactory;

    public HibernateOperatorRepository(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    public Operator create(Operator operator) {
        Objects.requireNonNull(operator);
        if (operator.getId() != null) {
            throw new IllegalArgumentException("Для create нужен оператор без ID");
        }
        return inTransaction(session -> {
            session.persist(operator);
            return operator;
        });
    }

    @Override
    public Optional<Operator> findById(Long id){
        Objects.requireNonNull(id);
        return inTransaction(session -> Optional.ofNullable(session.find(Operator.class, id)));
    }


    @Override
    public List<Operator> findAll() {
        return inTransaction(session -> session
            .createSelectionQuery("from Operator o order by o.id", Operator.class)
            .getResultList());
    }

    @Override
    public boolean update(Operator operator){
        Objects.requireNonNull(operator);
        Objects.requireNonNull(operator.getId());
        return inTransaction(session -> {
            Operator existing = session.find(Operator.class, operator.getId());
            if (existing == null){
                return false;
            }
            existing.setFullName(operator.getFullName());
            existing.setPhone(operator.getPhone());
            return true;
        });
    }

    @Override
    public boolean deleteById(Long id){
        Objects.requireNonNull(id);
        return inTransaction(session -> {
            Operator existing = session.find(Operator.class, id);
            if (existing == null){
                return false;
            }
            session.remove(existing);
            return true;
        });
    }


    private <T> T inTransaction(Function<Session, T> action) {
        try (Session session = sessionFactory.openSession()){
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
