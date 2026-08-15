package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A photo of a paper pledge card, sent as a base64 data URL
 * (e.g. {@code data:image/jpeg;base64,...}). The portal downscales the photo
 * client-side before uploading, so this stays well under a megabyte.
 */
public record PledgeCardScanRequest(
        @NotBlank String imageDataUrl
) {
}
