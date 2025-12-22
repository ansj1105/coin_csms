package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KoriPointAdjustRequestDto {
    private Long userId;
    private Double amount;
    private String type; // "ADD" or "WITHDRAW"
}

