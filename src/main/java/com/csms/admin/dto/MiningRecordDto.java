package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningRecordDto {
    private Long id;
    private Long userId;
    private String referrerNickname;
    private String nickname;
    private String email;
    private Integer level;
    private LocalDateTime miningStartTime;
    private LocalDateTime miningEndTime;
    private Double miningAmount;
    private Double cumulativeMiningAmount;
    private Integer miningEfficiency;
    private String activityStatus;
}

