package org.donorly.backend.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.model.Organization;
import org.donorly.backend.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OrgLogoService {

    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final String GCS_OBJECT_PATH = "orgs/%s/logo.webp";

    private final OrganizationRepository organizationRepository;

    @Value("${donorly.storage.gcs-bucket:}")
    private String gcsBucket;

    public OrgLogoService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /** API path served by this app, or external/GCS URL. Never returns base64. */
    public String resolveLogoUrl(Organization org) {
        if (org == null) return null;
        String base;
        if (isGcsPublicUrl(org.getLogoUrl())) {
            base = org.getLogoUrl();
        } else if (isExternalHttpUrl(org.getLogoUrl()) && !hasInternalBytes(org)) {
            base = org.getLogoUrl();
        } else if (hasInternalBytes(org) || isGcsObjectKey(org.getLogoUrl())) {
            base = "/api/organizations/" + org.getId() + "/logo";
        } else {
            return null;
        }
        return withCacheBuster(base, org);
    }

    private String withCacheBuster(String url, Organization org) {
        long v = org.getModifiedAt() != null ? org.getModifiedAt().getEpochSecond() : 0;
        return url + (url.contains("?") ? "&" : "?") + "v=" + v;
    }

    public boolean hasLogo(Organization org) {
        return resolveLogoUrl(org) != null;
    }

    public Optional<LogoPayload> loadLogo(UUID orgId) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null || org.getDeletedAt() != null) {
            return Optional.empty();
        }

        if (isGcsPublicUrl(org.getLogoUrl())) {
            return fetchFromGcsPublicUrl(org.getLogoUrl());
        }
        if (isGcsObjectKey(org.getLogoUrl()) && gcsBucket != null && !gcsBucket.isBlank()) {
            return fetchFromGcsBucket(org.getLogoUrl());
        }
        if (org.getLogoData() != null && !org.getLogoData().isBlank()) {
            return decodeDataUrl(org.getLogoData());
        }
        if (isExternalHttpUrl(org.getLogoUrl())) {
            return Optional.empty(); // controller will redirect
        }
        return Optional.empty();
    }

    public boolean isExternalOnly(Organization org) {
        return isExternalHttpUrl(org.getLogoUrl()) && !hasInternalBytes(org);
    }

    public String externalRedirectUrl(Organization org) {
        return isExternalHttpUrl(org.getLogoUrl()) ? org.getLogoUrl() : null;
    }

    /** ~2 MB decoded. Checked against the base64 length BEFORE decoding to avoid an allocation bomb. */
    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;

    @Transactional
    public void saveFromDataUrl(UUID orgId, String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) return;

        // Base64 inflates by 4/3, so cap the encoded string first: a multi-hundred-MB
        // payload must be rejected before Base64.decode materializes it on the heap.
        if (dataUrl.length() > MAX_LOGO_BYTES * 4L / 3 + 64) {
            throw new IllegalArgumentException("Logo image is too large (max 2 MB)");
        }

        LogoPayload payload = decodeDataUrl(dataUrl)
                .orElseThrow(() -> new IllegalArgumentException("Invalid logo image data"));
        if (payload.bytes().length > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo image is too large (max 2 MB)");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organization not found"));

        if (gcsBucket != null && !gcsBucket.isBlank()) {
            String objectName = GCS_OBJECT_PATH.formatted(orgId);
            uploadToGcs(objectName, payload.bytes(), payload.contentType());
            org.setLogoUrl(buildGcsPublicUrl(objectName));
            org.setLogoData(null);
        } else {
            org.setLogoData(dataUrl);
        }
        organizationRepository.save(org);
    }

    /** Moves legacy logo_data rows to GCS when bucket is configured. */
    @Transactional
    public int migrateLegacyLogoData() {
        if (gcsBucket == null || gcsBucket.isBlank()) {
            return 0;
        }
        int migrated = 0;
        for (Organization org : organizationRepository.findAll()) {
            if (org.getLogoData() == null || org.getLogoData().isBlank()) continue;
            try {
                saveFromDataUrl(org.getId(), org.getLogoData());
                migrated++;
                log.info("Migrated logo for org {} to GCS", org.getSlug());
            } catch (Exception e) {
                log.warn("Failed to migrate logo for org {}: {}", org.getSlug(), e.getMessage());
            }
        }
        return migrated;
    }

    private boolean hasInternalBytes(Organization org) {
        return (org.getLogoData() != null && !org.getLogoData().isBlank())
                || isGcsObjectKey(org.getLogoUrl());
    }

    private void uploadToGcs(String objectName, byte[] bytes, String contentType) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobInfo info = BlobInfo.newBuilder(BlobId.of(gcsBucket, objectName))
                .setContentType(contentType != null ? contentType : "image/webp")
                .setCacheControl("public, max-age=86400")
                .build();
        storage.create(info, bytes);
    }

    private Optional<LogoPayload> fetchFromGcsBucket(String objectName) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Blob blob = storage.get(BlobId.of(gcsBucket, objectName));
            if (blob == null) return Optional.empty();
            return Optional.of(new LogoPayload(blob.getContent(), blob.getContentType()));
        } catch (Exception e) {
            log.warn("GCS read failed for {}: {}", objectName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LogoPayload> fetchFromGcsPublicUrl(String url) {
        String prefix = "https://storage.googleapis.com/" + gcsBucket + "/";
        if (!url.startsWith(prefix)) {
            return Optional.empty();
        }
        String objectName = url.substring(prefix.length());
        return fetchFromGcsBucket(objectName);
    }

    private String buildGcsPublicUrl(String objectName) {
        return "https://storage.googleapis.com/" + gcsBucket + "/" + objectName;
    }

    private boolean isGcsPublicUrl(String url) {
        return url != null && url.startsWith("https://storage.googleapis.com/");
    }

    private boolean isGcsObjectKey(String url) {
        return url != null && url.startsWith("orgs/") && url.contains("/logo");
    }

    private boolean isExternalHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private Optional<LogoPayload> decodeDataUrl(String dataUrl) {
        Matcher m = DATA_URL.matcher(dataUrl.trim());
        if (!m.matches()) return Optional.empty();
        String contentType = m.group(1);
        byte[] bytes = Base64.getDecoder().decode(m.group(2).replaceAll("\\s", ""));
        return Optional.of(new LogoPayload(bytes, contentType));
    }

    public record LogoPayload(byte[] bytes, String contentType) {}
}
