package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingRewardDto {
    private RegionalReward regional;
    private NationalReward national;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionalReward {
        private Double rank1;
        private Double rank2;
        private Double rank3;
        private Double rank4to10;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NationalReward {
        private Double rank1;
        private Double rank2;
        private Double rank3;
        private Double rank4to10;
    }
}

