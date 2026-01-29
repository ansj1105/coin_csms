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
public class AirdropTransferDto {
    private Long id;
    private String transferId;
    private Long userId;
    private String loginId;
    private String nickname;
    private Long walletId;
    private Integer currencyId;
    private String currencyCode;
    private BigDecimal amount;
    private String status;
    private String orderNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
