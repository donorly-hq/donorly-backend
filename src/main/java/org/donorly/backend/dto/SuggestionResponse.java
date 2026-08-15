package org.donorly.backend.dto;

/**
 * One actionable dashboard suggestion. Keys are stable per situation (rules
 * about a specific campaign embed its id, e.g. {@code campaign-behind:<uuid>})
 * so a dismissal snoozes exactly that situation and nothing else.
 */
public record SuggestionResponse(
        String key,
        String severity,     // info | warning | critical
        String title,
        String message,
        String actionLabel,  // null when there is no self-serve action
        String actionRoute
) {
}
