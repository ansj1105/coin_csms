package com.csms.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateAirdropPhaseRequestDto {
    private BigDecimal amount;
    private LocalDateTime unlockDate;
    private Integer daysRemaining;
    private String status;  // PROCESSING, RELEASED
}
