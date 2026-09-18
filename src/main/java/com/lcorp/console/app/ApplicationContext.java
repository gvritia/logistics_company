package com.lcorp.console.app;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.config.HibernateConfig;
import com.lcorp.console.export.XlsxExporter;
import com.lcorp.console.repository.ClientRepository;
import com.lcorp.console.repository.DriverRepository;
import com.lcorp.console.repository.OperatorRepository;
import com.lcorp.console.repository.TransportationRequestQueryRepository;
import com.lcorp.console.repository.TransportationRequestRepository;
import com.lcorp.console.repository.hibernate.HibernateClientRepository;
import com.lcorp.console.repository.hibernate.HibernateDriverRepository;
import com.lcorp.console.repository.hibernate.HibernateOperatorRepository;
import com.lcorp.console.repository.hibernate.HibernateTransportationRequestRepository;
import com.lcorp.console.repository.jdbc.JdbcTransportationRequestQueryRepository;
import com.lcorp.console.service.ClientService;
import com.lcorp.console.service.DriverService;
import com.lcorp.console.service.OperatorService;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.console.service.TransportationRequestService;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public final class ApplicationContext implements AutoCloseable {

    private final SessionFactory sessionFactory;
    private final ClientService clientService;
    private final DriverService driverService;
    private final OperatorService operatorService;
    private final TransportationRequestService requestService;
    private final TransportationRequestQueryService queryService;
    private final XlsxExporter exporter = new XlsxExporter();

    public ApplicationContext(DatabaseConfig config) throws SQLException {
        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(config);
        verifyConnection(connectionFactory);

        this.sessionFactory = HibernateConfig.createSessionFactory(config);

        ClientRepository clientRepository = new HibernateClientRepository(sessionFactory);
        DriverRepository driverRepository = new HibernateDriverRepository(sessionFactory);
        OperatorRepository operatorRepository = new HibernateOperatorRepository(sessionFactory);
        TransportationRequestRepository requestRepository =
            new HibernateTransportationRequestRepository(sessionFactory);
        TransportationRequestQueryRepository queryRepository =
            new JdbcTransportationRequestQueryRepository(connectionFactory);

        this.requestService = new TransportationRequestService(
            requestRepository, clientRepository, operatorRepository, driverRepository
        );
        this.clientService = new ClientService(clientRepository, requestService);
        this.driverService = new DriverService(driverRepository, requestService);
        this.operatorService = new OperatorService(operatorRepository, requestService);
        this.queryService = new TransportationRequestQueryService(queryRepository);
    }

    public ClientService clients() {
        return clientService;
    }

    public DriverService drivers() {
        return driverService;
    }

    public OperatorService operators() {
        return operatorService;
    }

    public TransportationRequestService requests() {
        return requestService;
    }

    public TransportationRequestQueryService queries() {
        return queryService;
    }

    public XlsxExporter exporter() {
        return exporter;
    }

    @Override
    public void close() {
        sessionFactory.close();
    }

    private static void verifyConnection(DatabaseConnectionFactory connectionFactory)
            throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("PostgreSQL не подтвердил соединение");
            }
        }
    }
}
