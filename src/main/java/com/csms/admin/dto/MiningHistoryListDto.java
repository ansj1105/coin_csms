package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningHistoryListDto {
    private List<MiningHistoryItem> items;
    private Integer total;
    private Integer limit;
    private Integer offset;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiningHistoryItem {
        private Long id;
        private String referrerNickname;
        private String nickname;
        private Integer miningEfficiency;
        private Integer level;
        private String invitationCode;
        private Integer teamMemberCount;
        private Double totalMiningAmount;
        private Double referralRevenue;
        private Double totalMinedHoldings;
        private String activityStatus;
        private String sanctionStatus;
    }
}

