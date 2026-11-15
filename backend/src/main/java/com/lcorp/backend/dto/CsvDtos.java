package com.lcorp.backend.dto;

import java.util.List;

public final class CsvDtos {
    private CsvDtos() {
    }

    public enum Mode { CREATE_ONLY, UPSERT_BY_CODE }

    public record Issue(long row, String field, String message) {
    }

    public record Preview(int totalRows, int validRows, int invalidRows,
                          String sha256, List<Issue> issues) {
    }

    public record Apply(int totalRows, int created, int updated, int skipped,
                        List<Issue> issues) {
    }
}
