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
public class MiningBoosterDto {
    private List<Booster> boosters;
    private Summary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Booster {
        private String type;
        private String name;
        private Boolean isEnabled;
        private Integer efficiency;
        private Integer maxCount;
        private Integer perUnitEfficiency;
        private String note;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer totalEfficiency;
        private Double miningAmountPerMinute;
    }
}

