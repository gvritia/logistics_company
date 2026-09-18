package com.lcorp.console.export;

import java.nio.file.Path;

public record ExportResult(Path file, int requestCount) {
}
