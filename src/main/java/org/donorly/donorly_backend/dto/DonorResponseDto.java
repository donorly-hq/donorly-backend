package org.donorly.donorly_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The frontend expects pledge/payment fields directly on the donor
 * object (pledgeAmount, status, donationType, ambassadorId, etc.).
 * In the new schema these live on the separate Pledge entity. This
 * DTO merges a Donor with its primary Pledge for frontend
 * compatibility, without denormalizing the actual database.
 */
public class DonorResponseDto {
    public UUID id;
    public String name;
    public String phone;
    public String email;
    public String address; // not stored in new schema — always blank for now
    public BigDecimal pledgeAmount;
    public String donationType; // not modeled yet — defaults to "General"
    public String recurringType; // maps from Pledge.recurrenceFrequency
    public Integer duration; // not modeled yet — defaults to 1
    public String paymentMethod; // not modeled per-donor yet — defaults to blank
    public UUID ambassadorId; // pulled from the donor's Pledge
    public boolean corporateMatch; // not modeled yet — defaults to false
    public String employer; // not modeled yet — always blank
    public String status; // "Pledged" / "Collected", derived from pledge status
    public String createdAt;
}
