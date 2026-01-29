package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirdropPhaseDto {
    private Long id;
    private Long userId;
    private String loginId;
    private String nickname;
    private Integer phase;
    private String status;
    private BigDecimal amount;
    private Boolean claimed;
    private LocalDateTime unlockDate;
    private Integer daysRemaining;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
