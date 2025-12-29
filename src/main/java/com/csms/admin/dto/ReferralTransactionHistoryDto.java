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
public class ReferralTransactionHistoryDto {
    private Long id;
    private String referrerNickname;
    private String invitationCode;
    private String nickname;
    private Integer level;
    private Integer teamMemberCount;
    private BigDecimal referralRevenueBefore;
    private BigDecimal referralRevenue;
    private BigDecimal referralRevenueAfter;
    private String date;
    private String time;
    private LocalDateTime createdAt;
}

