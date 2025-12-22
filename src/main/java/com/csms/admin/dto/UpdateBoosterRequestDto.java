package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoosterRequestDto {
    private String type;
    private Boolean isEnabled;
    private Integer efficiency;
    private Integer maxCount;
    private Integer perUnitEfficiency;
}

