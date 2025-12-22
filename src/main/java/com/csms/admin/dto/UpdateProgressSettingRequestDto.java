package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressSettingRequestDto {
    private BroadcastSettingUpdate broadcastProgress;
    private BroadcastSettingUpdate broadcastListening;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastSettingUpdate {
        private Boolean isEnabled;
        private Integer timePerHour;
        private Double coinsPerHour;
    }
}

