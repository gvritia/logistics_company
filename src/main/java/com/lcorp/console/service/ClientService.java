package com.lcorp.console.service;

import com.lcorp.console.exception.EntityNotFoundException;
import com.lcorp.console.model.Client;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.repository.ClientRepository;

import java.util.List;
import java.util.Objects;

// Управление клиентами логистической компании
public class ClientService {

    private final ClientRepository clientRepository;
    private final TransportationRequestService requestService;

    public ClientService(
            ClientRepository clientRepository,
            TransportationRequestService requestService
    ) {
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.requestService = Objects.requireNonNull(requestService);
    }

    public Client createClient(Client client) {
        validateNewClient(client);
        return clientRepository.create(client);
    }

    public Client getById(Long clientId) {
        return requireClient(clientId);
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client updateClient(Client client) {
        validateExistingClient(client);
        requireClient(client.getId());

        if (!clientRepository.update(client)) {
            throw new EntityNotFoundException("Клиент", client.getId());
        }
        return client;
    }

    public void deleteClient(Long clientId) {
        requireClient(clientId);

        // Клиента со связанными заявками нельзя удалять, иначе потеряется
        // связь с историей перевозок
        if (!findRequestsForExistingClient(clientId).isEmpty()) {
            throw new IllegalStateException(
                    "Нельзя удалить клиента, у которого есть заявки"
            );
        }

        if (!clientRepository.deleteById(clientId)) {
            throw new EntityNotFoundException("Клиент", clientId);
        }
    }

    // В Repository пока нет отдельного запроса findByClientId, поэтому на
    // текущем этапе фильтрация выполняется через сервис заявок
    public List<TransportationRequest> findRequests(Long clientId) {
        requireClient(clientId);
        return findRequestsForExistingClient(clientId);
    }

    private List<TransportationRequest> findRequestsForExistingClient(Long clientId) {
        return requestService.findAll().stream()
                .filter(request -> Objects.equals(request.getClientId(), clientId))
                .toList();
    }

    private void validateNewClient(Client client) {
        requireClientObject(client);
        if (client.getId() != null) {
            throw new IllegalArgumentException("У нового клиента ID должен отсутствовать");
        }
        validateClientFields(client);
    }

    private void validateExistingClient(Client client) {
        requireClientObject(client);
        requirePositiveId(client.getId());
        validateClientFields(client);
    }

    private void validateClientFields(Client client) {
        if (client.getFullName() == null || client.getFullName().isBlank()) {
            throw new IllegalArgumentException("ФИО клиента обязательно");
        }

        boolean phoneMissing = client.getPhone() == null || client.getPhone().isBlank();
        boolean emailMissing = client.getEmail() == null || client.getEmail().isBlank();
        if (phoneMissing && emailMissing) {
            throw new IllegalArgumentException(
                    "У клиента должен быть указан телефон или email"
            );
        }

        if (!emailMissing && !isEmailValid(client.getEmail())) {
            throw new IllegalArgumentException("Email клиента имеет неверный формат");
        }
    }

    private boolean isEmailValid(String email) {
        int atIndex = email.indexOf('@');
        int dotIndex = email.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < email.length() - 1;
    }

    private Client requireClient(Long clientId) {
        requirePositiveId(clientId);
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
    }

    private void requireClientObject(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Клиент не должен быть null");
        }
    }

    private void requirePositiveId(Long clientId) {
        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("ID клиента должен быть положительным");
        }
    }
}
