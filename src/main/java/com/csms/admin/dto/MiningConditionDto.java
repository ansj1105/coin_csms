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
public class MiningConditionDto {
    private BasicConditions basicConditions;
    private ProgressSettings progressSettings;
    private List<LevelLimit> levelLimits;
    private Boolean levelLimitsEnabled;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicConditions {
        private Boolean isEnabled;
        private Boolean baseTimeEnabled;
        private Integer baseTimeMinutes;
        private List<Mission> missions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mission {
        private String type;
        private String name;
        private Integer requiredCount;
        private Boolean isEnabled;
        private Boolean hasInput;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressSettings {
        private BroadcastSetting broadcastProgress;
        private BroadcastSetting broadcastListening;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastSetting {
        private Boolean isEnabled;
        private Integer timePerHour;
        private Double coinsPerHour;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelLimit {
        private Integer level;
        private Double dailyLimit;
    }
}

