package org.donorly.backend.model;

/**
 * Lifecycle of a pledge. Persisted as its lowercase string value (the DB column and
 * API payloads are plain strings), so this enum is the catalog of valid values rather
 * than a change to the storage format.
 */
public enum PledgeStatus {
    PENDING("pending"),
    ACTIVE("active"),
    FULFILLED("fulfilled"),
    CANCELLED("cancelled");

    private final String value;

    PledgeStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean matches(String raw) {
        return value.equals(raw);
    }

    /** Returns null for unknown/legacy values instead of throwing. */
    public static PledgeStatus fromValue(String raw) {
        for (PledgeStatus s : values()) {
            if (s.value.equals(raw)) return s;
        }
        return null;
    }

    public static boolean isValid(String raw) {
        return fromValue(raw) != null;
    }
}
