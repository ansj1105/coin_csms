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
public class MiningHistoryDetailDto {
    private Long userId;
    private String userNickname;
    private Integer userLevel;
    private List<MiningHistoryRecord> records;
    private Integer total;
    private Integer limit;
    private Integer offset;
    private Summary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiningHistoryRecord {
        private Long id;
        private Integer year;
        private String date;
        private String time;
        private Integer level;
        private String miningType;
        private Integer miningEfficiency;
        private String invitationCode;
        private Integer teamMemberCount;
        private Double miningAmount;
        private Double referralRevenue;
        private Double totalMinedHoldings;
        private String deviceInfo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Double totalMiningAmount;
        private Double totalReferralRevenue;
        private Double totalMinedHoldings;
    }
}

