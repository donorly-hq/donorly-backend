package org.donorly.donorly_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The frontend expects "id", "code", "totalPledged" — none of which
 * are the actual field names on the Ambassador entity anymore. This
 * DTO is the translation layer so the backend can keep its clean
 * schema while the existing portal keeps working unmodified.
 */
public class AmbassadorResponseDto {
    public UUID id;
    public String name;
    public String email;
    public String phone;
    public String city; // not stored in new schema — always blank for now
    public String code;
    public BigDecimal pledgeGoal;
    public BigDecimal totalPledged;
    public String status; // "Active" / "Inactive"
    public String createdAt;
    public UUID parentAmbassadorId;
}
