package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinAdjustRequestDto {
    private Long userId;
    private String network; // optional
    private String token; // optional
    private Double amount;
    private String type; // "ADD" or "WITHDRAW"
}

