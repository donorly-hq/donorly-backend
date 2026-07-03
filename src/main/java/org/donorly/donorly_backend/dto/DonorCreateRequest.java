package org.donorly.donorly_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DonorCreateRequest {
    public String name;
    public String phone;
    public String email;
    public String address;
    public BigDecimal pledgeAmount;
    public String donationType;
    public String recurringType;
    public Integer duration;
    public String paymentMethod;
    public UUID ambassadorId;
    public boolean corporateMatch;
    public String employer;
    public String status;
}
