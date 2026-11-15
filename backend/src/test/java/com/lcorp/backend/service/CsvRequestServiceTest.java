package com.lcorp.backend.service;

import com.lcorp.backend.domain.Client;
import com.lcorp.backend.dto.CsvDtos;
import com.lcorp.backend.repository.ClientRepository;
import com.lcorp.backend.repository.OperatorRepository;
import com.lcorp.backend.repository.TransportationRequestRepository;
import com.lcorp.backend.dto.CsvDtos.Mode;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvRequestServiceTest {
    @Test
    void spreadsheetProtectionPreservesLeadingApostropheOnReimport() {
        for (String original : new String[]{"=2+2", "'=2+2", "''=2+2", "ordinary"}) {
            assertEquals(original, CsvRequestService.text(CsvRequestService.safeCell(original)));
        }
        assertEquals("'=2+2", CsvRequestService.safeCell("=2+2"));
        assertEquals("''=2+2", CsvRequestService.safeCell("'=2+2"));
    }

    @Test
    void previewFindsDuplicateCodesAndBindsApplyToSameFile() {
        var requests = mock(TransportationRequestRepository.class);
        var clients = mock(ClientRepository.class);
        var operators = mock(OperatorRepository.class);
        when(requests.findByRequestCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(clients.findById(1L)).thenReturn(Optional.of(new Client("Test", "70000000000", null)));
        when(operators.existsById(1L)).thenReturn(true);
        String header = "request_code,client_id,operator_id,cargo_description,weight_kg,price,"
            + "pickup_address,delivery_address,planned_delivery_at,fragile,status,tags,version\n";
        String row = "CSV-TEST,1,1,Cargo,1000,100,Moscow,Kazan,"
            + LocalDateTime.now().plusDays(2) + ",false,CREATED,fragile|heavy,\n";
        var file = new MockMultipartFile("file", "test.csv", "text/csv",
            (header + row + row).getBytes(StandardCharsets.UTF_8));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new CsvRequestService(requests, clients, operators,
                mock(RequestService.class), factory.getValidator());
            CsvDtos.Preview preview = service.preview(file, Mode.CREATE_ONLY);
            assertEquals(2, preview.totalRows());
            assertEquals(1, preview.validRows());
            assertEquals(1, preview.invalidRows());
            assertEquals("request_code", preview.issues().getFirst().field());
            assertThrows(IllegalArgumentException.class,
                () -> service.apply(file, Mode.CREATE_ONLY, "0".repeat(64), null, "test"));
        }
    }
}
