package com.lcorp.console.repository.hibernate;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.repository.TransportationRequestRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class HibernateTransportationRequestRepository implements TransportationRequestRepository {

    private final SessionFactory sessionFactory;

    public HibernateTransportationRequestRepository(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    public TransportationRequest create(TransportationRequest request) {
        Objects.requireNonNull(request);
        if (request.getId() != null) {
            throw new IllegalArgumentException("Для create нужна заявка без ID");
        }
        return inTransaction(session -> {
            // IDENTITY записывает выданный PostgreSQL ID в переданный объект
            session.persist(request);
            return request;
        });
    }

    @Override
    public Optional<TransportationRequest> findById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session ->
            Optional.ofNullable(session.find(TransportationRequest.class, id)));
    }

    @Override
    public List<TransportationRequest> findAll() {
        return inTransaction(session -> session
            .createSelectionQuery("from TransportationRequest r order by r.id", TransportationRequest.class)
            .getResultList());
    }

    @Override
    public boolean update(TransportationRequest request) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(request.getId());
        return inTransaction(session -> {
            TransportationRequest existing = session.find(TransportationRequest.class, request.getId());
            if (existing == null) {
                return false;
            }

            existing.setClientId(request.getClientId());
            existing.setDriverId(request.getDriverId());
            existing.setOperatorId(request.getOperatorId());
            existing.setCargoDescription(request.getCargoDescription());
            existing.setWeightKg(request.getWeightKg());
            existing.setPrice(request.getPrice());
            existing.setCreatedAt(request.getCreatedAt());
            existing.setPlannedDeliveryAt(request.getPlannedDeliveryAt());
            existing.setStatus(request.getStatus());
            existing.setPickupAddress(request.getPickupAddress());
            existing.setDeliveryAddress(request.getDeliveryAddress());
            return true;
        });
    }

    @Override
    public boolean deleteById(Long id) {
        Objects.requireNonNull(id);
        return inTransaction(session -> {
            TransportationRequest existing = session.find(TransportationRequest.class, id);
            if (existing == null) {
                return false;
            }
            session.remove(existing);
            return true;
        });
    }

    @Override
    public List<TransportationRequest> findAvailable() {
        // Свободна только одобренная заявка без доставщика; одного NULL недостаточно
        return inTransaction(session -> session
            .createSelectionQuery(
                "from TransportationRequest r where r.status = :status "
                    + "and r.driverId is null order by r.id",
                TransportationRequest.class
            )
            .setParameter("status", TransportationRequestStatus.APPROVED)
            .getResultList());
    }

    @Override
    public List<TransportationRequest> findByDriverId(Long driverId) {
        Objects.requireNonNull(driverId);
        // Именованный параметр передаёт значение отдельно от текста запроса
        // Статус не ограничиваем: история завершённых/отменённых заявок тоже нужна
        return inTransaction(session -> session
            .createSelectionQuery(
                "from TransportationRequest r where r.driverId = :driverId order by r.id",
                TransportationRequest.class
            )
            .setParameter("driverId", driverId)
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
