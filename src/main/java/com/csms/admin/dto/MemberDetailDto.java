package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetailDto {
    private Long id;
    private String loginId;
    private Long referrerId;
    private String referrerNickname;
    private String nickname;
    private String email;
    private Integer level;
    private String invitationCode;
    private Integer teamMemberCount;
    private Double referralRevenue;
    private Double totalMinedAmount;
    private String activityStatus;
    private String sanctionStatus;
    private String gender;
    private Integer age;
    private String realName;
    private LocalDate birthDate;
    private String phone;
    private String kakaoId;
    private LocalDateTime registeredAt;
    private Boolean hasPaymentHistory;
    private Boolean hasDepositHistory;
    private String lastIpAddress;
    private LocalDateTime lastLoginAt;
    private List<WalletInfo> wallets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletInfo {
        private Long id;
        private String currencyCode;
        private String address;
        private Double balance;
        private String status;
    }
}

