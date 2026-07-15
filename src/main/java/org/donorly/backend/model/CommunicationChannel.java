package org.donorly.backend.model;

/**
 * Delivery channel for outbound communications. Persisted as its lowercase string
 * value — this enum is the catalog of valid values, not a storage-format change.
 */
public enum CommunicationChannel {
    EMAIL("email"),
    SMS("sms");

    private final String value;

    CommunicationChannel(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean matches(String raw) {
        return value.equals(raw);
    }

    /** Returns null for unknown values instead of throwing. */
    public static CommunicationChannel fromValue(String raw) {
        for (CommunicationChannel c : values()) {
            if (c.value.equals(raw)) return c;
        }
        return null;
    }

    public static boolean isValid(String raw) {
        return fromValue(raw) != null;
    }
}
