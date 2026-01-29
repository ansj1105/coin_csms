package com.csms.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAirdropPhaseRequestDto {
    private Long userId;
    private Integer phase;  // 1~5
    private BigDecimal amount;
    private LocalDateTime unlockDate;  // unlockDate 또는 daysRemaining 중 하나
    private Integer daysRemaining;  // unlockDate가 없으면 현재 날짜 + daysRemaining으로 계산
}
