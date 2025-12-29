package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralTreeMemberDto {
    private Long id;
    private Long referrerId;
    private String referrerNickname;
    private String invitationCode;
    private String nickname;
    private Integer level;
    private Integer teamMemberCount;
    private BigDecimal totalReferralRevenue;
    private BigDecimal referralRevenue; // 하부 회원만
    private String referralRegistrationDate; // 하부 회원만
    private String referralRegistrationTime; // 하부 회원만
    private String activityStatus;
    private List<ReferralTreeMemberDto> children; // 상위 회원만
}

