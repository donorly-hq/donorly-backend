package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.service.OrgLogoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationLogoController {

    private final OrganizationRepository organizationRepository;
    private final OrgLogoService orgLogoService;

    /** Public org logo — used for watermarks (no auth header in CSS/img requests). */
    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        Organization org = organizationRepository.findById(id).orElse(null);
        if (org == null || org.getDeletedAt() != null) {
            return ResponseEntity.notFound().build();
        }

        if (orgLogoService.isExternalOnly(org)) {
            String redirect = orgLogoService.externalRedirectUrl(org);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
        }

        if (orgLogoService.resolveLogoUrl(org) != null
                && orgLogoService.resolveLogoUrl(org).startsWith("https://storage.googleapis.com/")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(orgLogoService.resolveLogoUrl(org)))
                    .build();
        }

        return orgLogoService.loadLogo(id)
                .map(payload -> ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .contentType(MediaType.parseMediaType(
                                payload.contentType() != null ? payload.contentType() : "image/jpeg"))
                        .body(payload.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
