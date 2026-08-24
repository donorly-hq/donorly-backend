package org.donorly.backend.dto;

import java.util.List;

public record PledgeCardImportResult(
        int imported,
        int skipped,
        List<String> errors
) {
}
