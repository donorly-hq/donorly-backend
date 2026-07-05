package org.donorly.backend.dto;

import java.util.List;

public record DonorImportResult(
        int imported,
        int skipped,
        List<String> errors
) {
}
