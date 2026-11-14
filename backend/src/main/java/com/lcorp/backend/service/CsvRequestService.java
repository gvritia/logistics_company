package com.lcorp.backend.service;

import com.lcorp.backend.domain.Client;
import com.lcorp.backend.domain.TransportationRequest;
import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.dto.CsvDtos;
import com.lcorp.backend.dto.RequestDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import com.lcorp.backend.repository.ClientRepository;
import com.lcorp.backend.repository.OperatorRepository;
import com.lcorp.backend.repository.TransportationRequestRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CsvRequestService {
    private static final int MAX_BYTES = 2_000_000;
    private static final int MAX_ROWS = 1_000;
    private static final List<String> HEADERS = List.of("request_code", "client_id", "operator_id",
        "cargo_description", "weight_kg", "price", "pickup_address", "delivery_address",
        "planned_delivery_at", "fragile", "status", "tags", "version");

    private final TransportationRequestRepository requests;
    private final ClientRepository clients;
    private final OperatorRepository operators;
    private final RequestService requestService;
    private final Validator validator;

    public CsvRequestService(TransportationRequestRepository requests, ClientRepository clients,
                             OperatorRepository operators, RequestService requestService, Validator validator) {
        this.requests = requests;
        this.clients = clients;
        this.operators = operators;
        this.requestService = requestService;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public CsvDtos.Preview preview(MultipartFile file, CsvDtos.Mode mode) {
        Parsed parsed = parse(read(file), mode);
        return new CsvDtos.Preview(parsed.total(), parsed.rows().size(), parsed.issues().size(),
            parsed.sha256(), parsed.issues());
    }

    // Нет общей транзакции: каждая корректная строка применяется через RequestService отдельно.
    public CsvDtos.Apply apply(MultipartFile file, CsvDtos.Mode mode, String expectedSha256,
                               UUID actorId, String correlationId) {
        byte[] bytes = read(file);
        if (expectedSha256 == null || !expectedSha256.matches("(?i)[0-9a-f]{64}")
            || !sha256(bytes).equalsIgnoreCase(expectedSha256)) {
            throw new IllegalArgumentException("Файл отличается от проверенного при preview");
        }
        Parsed parsed = parse(bytes, mode);
        List<CsvDtos.Issue> issues = new ArrayList<>(parsed.issues());
        int created = 0;
        int updated = 0;
        for (CsvRow row : parsed.rows()) {
            try {
                if (row.existingId() == null) {
                    requestService.create(row.create(), actorId, correlationId);
                    created++;
                } else {
                    requestService.update(row.existingId(), row.update(), actorId, correlationId);
                    updated++;
                }
            } catch (ConflictFailure | NotFoundFailure | IllegalArgumentException ex) {
                issues.add(new CsvDtos.Issue(row.number(), "row", ex.getMessage()));
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                issues.add(new CsvDtos.Issue(row.number(), "row", "Данные изменились; повторите preview"));
            }
        }
        return new CsvDtos.Apply(parsed.total(), created, updated, issues.size(), List.copyOf(issues));
    }

    @Transactional(readOnly = true)
    public void export(RequestSearch search, OutputStream output) throws IOException {
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                 .setHeader(HEADERS.toArray(String[]::new)).get())) {
            for (int page = 0; ; page++) {
                var results = requests.findAll(search.specification(),
                    PageRequest.of(page, 50, search.safeSort()));
                for (TransportationRequest request : results.getContent()) {
                    printer.printRecord(request.getRequestCode(), request.getClient().getId(),
                        request.getOperator().getId(), safeCell(request.getCargoDescription()),
                        request.getWeightKg(), request.getPrice(), safeCell(request.getPickupAddress()),
                        safeCell(request.getDeliveryAddress()), request.getPlannedDeliveryAt(),
                        request.isFragile(), request.getStatus(), safeCell(encodeTags(request.getTags())),
                        request.getVersion());
                }
                if (results.isLast()) break;
            }
            printer.flush();
        }
    }

    private Parsed parse(byte[] bytes, CsvDtos.Mode mode) {
        if (mode == null) throw new IllegalArgumentException("Укажите режим импорта");
        String source;
        try {
            source = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException("CSV должен быть в UTF-8");
        }
        if (source.startsWith("\ufeff")) source = source.substring(1);
        List<CsvRow> rows = new ArrayList<>();
        List<CsvDtos.Issue> issues = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        int total = 0;
        try (CSVParser csv = CSVParser.parse(new StringReader(source), CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get())) {
            if (!csv.getHeaderNames().equals(HEADERS)) {
                throw new IllegalArgumentException("Заголовки CSV должны совпадать с шаблоном экспорта");
            }
            for (CSVRecord record : csv) {
                if (++total > MAX_ROWS) throw new IllegalArgumentException("CSV содержит более 1000 строк");
                long rowNumber = record.getRecordNumber() + 1;
                try {
                    if (record.size() != HEADERS.size()) throw new RowProblem("row", "Неверное число колонок");
                    CsvRow row = row(record, rowNumber, mode);
                    if (!codes.add(row.create().requestCode().toUpperCase(Locale.ROOT))) {
                        throw new RowProblem("request_code", "Повтор кода внутри CSV");
                    }
                    rows.add(row);
                } catch (RowProblem ex) {
                    issues.add(new CsvDtos.Issue(rowNumber, ex.field, ex.getMessage()));
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Некорректный CSV: " + ex.getMessage());
        }
        return new Parsed(total, List.copyOf(rows), List.copyOf(issues), sha256(bytes));
    }

    private CsvRow row(CSVRecord record, long number, CsvDtos.Mode mode) {
        String code = text(record.get("request_code")).toUpperCase(Locale.ROOT);
        long clientId = positiveId(record.get("client_id"), "client_id");
        long operatorId = positiveId(record.get("operator_id"), "operator_id");
        BigDecimal weight = decimal(record.get("weight_kg"), "weight_kg");
        BigDecimal price = decimal(record.get("price"), "price");
        LocalDateTime planned;
        try { planned = LocalDateTime.parse(record.get("planned_delivery_at").trim()); }
        catch (DateTimeParseException ex) { throw new RowProblem("planned_delivery_at", "Ожидается ISO-дата и время"); }
        String rawFragile = record.get("fragile").trim();
        if (!rawFragile.equalsIgnoreCase("true") && !rawFragile.equalsIgnoreCase("false")) {
            throw new RowProblem("fragile", "Ожидается true или false");
        }
        boolean fragile = Boolean.parseBoolean(rawFragile);
        if (!record.get("status").trim().equals("CREATED")) {
            throw new RowProblem("status", "CSV изменяет только заявки в статусе CREATED");
        }
        Set<String> tags = decodeTags(text(record.get("tags")));
        RequestDtos.Create create = new RequestDtos.Create(code, clientId, operatorId,
            text(record.get("cargo_description")), weight, price, text(record.get("pickup_address")),
            text(record.get("delivery_address")), planned, fragile, tags);
        violations(validator.validate(create), number);

        TransportationRequest existing = requests.findByRequestCodeIgnoreCase(code).orElse(null);
        String rawVersion = record.get("version").trim();
        if (existing != null && mode == CsvDtos.Mode.CREATE_ONLY) {
            throw new RowProblem("request_code", "Заявка с таким кодом уже существует");
        }
        if (existing == null && !rawVersion.isEmpty()) {
            throw new RowProblem("version", "У новой заявки версия должна быть пустой");
        }
        if (existing != null && (existing.isArchived()
            || existing.getStatus() != TransportationRequestStatus.CREATED)) {
            throw new RowProblem("status", "Обновлять можно только активную заявку CREATED");
        }
        long version = existing == null ? 0 : nonnegative(rawVersion, "version");
        if (existing != null && existing.getVersion() != version) {
            throw new RowProblem("version", "Версия заявки устарела");
        }
        Client client = clients.findById(clientId).orElseThrow(() -> new RowProblem("client_id", "Клиент не найден"));
        if (!client.isActive() && (existing == null || !clientIdEquals(existing, clientId))) {
            throw new RowProblem("client_id", "Клиент отключён");
        }
        if (!operators.existsById(operatorId)) throw new RowProblem("operator_id", "Оператор не найден");
        if (create.pickupAddress().trim().equalsIgnoreCase(create.deliveryAddress().trim())) {
            throw new RowProblem("delivery_address", "Адреса должны отличаться");
        }
        LocalDateTime created = existing == null ? LocalDateTime.now() : existing.getCreatedAt();
        if (!planned.isAfter(created)) throw new RowProblem("planned_delivery_at", "Доставка должна быть позже создания");
        RequestDtos.Update update = existing == null ? null : new RequestDtos.Update(version, code,
            clientId, operatorId, create.cargoDescription(), weight, price, create.pickupAddress(),
            create.deliveryAddress(), planned, fragile, tags);
        return new CsvRow(number, create, update, existing == null ? null : existing.getPublicId());
    }

    private static boolean clientIdEquals(TransportationRequest request, long clientId) {
        return request.getClient().getId() == clientId;
    }

    private static void violations(Set<? extends ConstraintViolation<?>> violations, long row) {
        if (!violations.isEmpty()) {
            var first = violations.iterator().next();
            throw new RowProblem(first.getPropertyPath().toString(), first.getMessage());
        }
    }

    private static long positiveId(String value, String field) {
        long result = nonnegative(value, field);
        if (result == 0) throw new RowProblem(field, "ID должен быть положительным");
        return result;
    }

    private static long nonnegative(String value, String field) {
        try {
            long result = Long.parseLong(value.trim());
            if (result < 0) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException ex) { throw new RowProblem(field, "Ожидается целое неотрицательное число"); }
    }

    private static BigDecimal decimal(String value, String field) {
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException ex) { throw new RowProblem(field, "Ожидается десятичное число"); }
    }

    private static Set<String> decodeTags(String encoded) {
        Set<String> tags = new LinkedHashSet<>();
        if (encoded.isEmpty()) return tags;
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (char ch : encoded.toCharArray()) {
            if (escaped) {
                if (ch != '\\' && ch != '|') throw new RowProblem("tags", "Некорректное экранирование тега");
                current.append(ch);
                escaped = false;
            }
            else if (ch == '\\') escaped = true;
            else if (ch == '|') { addTag(tags, current.toString()); current.setLength(0); }
            else current.append(ch);
        }
        if (escaped) throw new RowProblem("tags", "Незавершённое экранирование тега");
        addTag(tags, current.toString());
        if (tags.size() > 20) throw new RowProblem("tags", "Более 20 тегов");
        return tags;
    }

    private static void addTag(Set<String> tags, String value) {
        String tag = value.trim();
        if (tag.isEmpty() || tag.length() > 50) throw new RowProblem("tags", "Некорректный тег");
        tags.add(tag);
    }

    private static String encodeTags(Set<String> tags) {
        return tags.stream().sorted().map(tag -> tag.replace("\\", "\\\\").replace("|", "\\|"))
            .reduce((left, right) -> left + "|" + right).orElse("");
    }

    static String safeCell(String value) {
        return formulaLike(value) ? "'" + value : value;
    }

    static String text(String value) {
        String result = value.trim();
        if (result.startsWith("'") && formulaLike(result.substring(1))) result = result.substring(1);
        return result;
    }

    private static boolean formulaLike(String value) {
        String leading = value.stripLeading();
        int index = 0;
        while (index < leading.length() && leading.charAt(index) == '\'') index++;
        return index < leading.length() && "=+-@".indexOf(leading.charAt(index)) >= 0;
    }

    private static byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Выберите CSV-файл размером до 2 МБ");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("CSV превышает 2 МБ");
            return bytes;
        }
        catch (IOException ex) { throw new IllegalArgumentException("Не удалось прочитать CSV"); }
    }

    private static String sha256(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 недоступен", ex); }
    }

    private record CsvRow(long number, RequestDtos.Create create, RequestDtos.Update update, UUID existingId) {
    }
    private record Parsed(int total, List<CsvRow> rows, List<CsvDtos.Issue> issues, String sha256) {
    }
    private static final class RowProblem extends RuntimeException {
        private final String field;
        private RowProblem(String field, String message) { super(message); this.field = field; }
    }
}
