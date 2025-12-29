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
public class WithdrawalRequestDto {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String loginId;
    private String nickname;
    private Integer year;
    private String date;
    private String time;
    private String type;
    private String asset;
    private String network;
    private String currencyCode;
    private BigDecimal requestAmount;
    private BigDecimal spread;
    private BigDecimal feeRate;
    private BigDecimal realtimePrice;
    private BigDecimal settlementAmount;
    private BigDecimal feeRevenue;
    private String walletAddress;
    private String status;
    private LocalDateTime createdAt;
}

