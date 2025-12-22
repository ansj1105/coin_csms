package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRankingRewardRequestDto {
    private String type; // REGIONAL or NATIONAL
    private Double rank1;
    private Double rank2;
    private Double rank3;
    private Double rank4to10;
}

