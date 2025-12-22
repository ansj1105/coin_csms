package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberListDto {
    private List<MemberInfo> members;
    private Long total;
    private Integer limit;
    private Integer offset;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
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
        private LocalDateTime registeredAt;
    }
}

