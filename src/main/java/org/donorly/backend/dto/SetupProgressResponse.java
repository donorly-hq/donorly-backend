package org.donorly.backend.dto;

import java.util.List;

/**
 * Onboarding checklist for one organization. Items are derived from live data
 * (counts, settings, branding) rather than stored flags, so the checklist can
 * never drift out of sync with the actual state of the org.
 */
public record SetupProgressResponse(
        int percent,
        int completedCount,
        int totalCount,
        List<Item> items
) {
    /**
     * @param ctaRoute frontend route for the tile's call-to-action; {@code null}
     *                 when the step cannot be self-served (e.g. branding is
     *                 managed by the platform team).
     */
    public record Item(
            String key,
            String title,
            String description,
            boolean complete,
            String ctaLabel,
            String ctaRoute
    ) {
    }
}
