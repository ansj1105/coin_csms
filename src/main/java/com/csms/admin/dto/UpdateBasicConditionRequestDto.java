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
public class UpdateBasicConditionRequestDto {
    private Boolean isEnabled;
    private Boolean baseTimeEnabled;
    private Integer baseTimeMinutes;
    private List<MissionUpdate> missions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionUpdate {
        private String type;
        private Integer requiredCount;
        private Boolean isEnabled;
    }
}

