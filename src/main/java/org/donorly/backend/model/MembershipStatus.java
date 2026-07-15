package org.donorly.backend.model;

/**
 * Status of an organization membership. Persisted as its lowercase string value —
 * this enum is the catalog of valid values, not a storage-format change.
 */
public enum MembershipStatus {
    ACTIVE("active"),
    INVITED("invited"),
    DISABLED("disabled");

    private final String value;

    MembershipStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean matches(String raw) {
        return value.equals(raw);
    }

    /** Returns null for unknown/legacy values instead of throwing. */
    public static MembershipStatus fromValue(String raw) {
        for (MembershipStatus s : values()) {
            if (s.value.equals(raw)) return s;
        }
        return null;
    }

    public static boolean isValid(String raw) {
        return fromValue(raw) != null;
    }
}
