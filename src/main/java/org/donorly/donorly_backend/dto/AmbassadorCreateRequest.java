package org.donorly.donorly_backend.dto;

import java.math.BigDecimal;

public class AmbassadorCreateRequest {
    public String name;
    public String email;
    public String phone;
    public String city;
    public String code;
    public BigDecimal pledgeGoal;
}
