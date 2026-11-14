package com.lcorp.backend.controller;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import com.lcorp.backend.dto.CsvDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.CsvRequestService;
import com.lcorp.backend.service.RequestSearch;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/requests")
public class CsvController {
    private final CsvRequestService csv;

    public CsvController(CsvRequestService csv) { this.csv = csv; }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public CsvDtos.Preview preview(@RequestPart("file") MultipartFile file,
                                   @RequestParam(defaultValue = "CREATE_ONLY") CsvDtos.Mode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public CsvDtos.Apply apply(@RequestPart("file") MultipartFile file,
                               @RequestParam(defaultValue = "CREATE_ONLY") CsvDtos.Mode mode,
                               @RequestParam String expectedSha256,
                               @AuthenticationPrincipal AuthPrincipal actor,
                               HttpServletRequest request) {
        return csv.apply(file, mode, expectedSha256, actor.id(), CorrelationIdFilter.get(request));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> export(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) Long clientId,
                                                         @RequestParam(required = false) TransportationRequestStatus status,
                                                         @RequestParam(required = false) Boolean fragile,
                                                         @RequestParam(required = false) WeightCategory weightCategory,
                                                         @RequestParam(required = false) String tag,
                                                         @RequestParam(defaultValue = "false") boolean archived,
                                                         @RequestParam(required = false) String weightOp,
                                                         @RequestParam(required = false) BigDecimal weightValue,
                                                         @RequestParam(defaultValue = "createdAt,desc") String sort) {
        RequestSearch search = new RequestSearch(q, clientId, status, fragile, weightCategory,
            tag, archived, weightOp, weightValue, 0, 50, sort);
        StreamingResponseBody body = output -> csv.export(search, output);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transportation-requests.csv")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(body);
    }
}
