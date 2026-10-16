package com.lcorp.javafx;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.repository.jdbc.JdbcTransportationRequestQueryRepository;
import com.lcorp.console.service.ClientService;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.console.service.TransportationRequestService;
import com.lcorp.javafx.repository.JdbcClientRepository;
import com.lcorp.javafx.repository.JdbcDriverRepository;
import com.lcorp.javafx.repository.JdbcOperatorRepository;
import com.lcorp.javafx.repository.JdbcRequestRepository;

public final class DesktopContext {
    private final JdbcClientRepository clients;
    private final JdbcDriverRepository drivers;
    private final JdbcOperatorRepository operators;
    private final TransportationRequestService requests;
    private final TransportationRequestQueryService queries;
    private final ClientService clientService;

    public DesktopContext() {
        DatabaseConnectionFactory database = new DatabaseConnectionFactory(DatabaseConfig.load());
        clients = new JdbcClientRepository(database);
        drivers = new JdbcDriverRepository(database);
        operators = new JdbcOperatorRepository(database);
        JdbcRequestRepository requestRepository = new JdbcRequestRepository(database);
        requests = new TransportationRequestService(requestRepository, clients, operators, drivers);
        queries = new TransportationRequestQueryService(
            new JdbcTransportationRequestQueryRepository(database));
        clientService = new ClientService(clients, requests);
    }

    public JdbcClientRepository clients() { return clients; }
    public JdbcDriverRepository drivers() { return drivers; }
    public JdbcOperatorRepository operators() { return operators; }
    public TransportationRequestService requests() { return requests; }
    public TransportationRequestQueryService queries() { return queries; }
    public ClientService clientService() { return clientService; }
}
