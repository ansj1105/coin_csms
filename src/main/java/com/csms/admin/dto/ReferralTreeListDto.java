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
public class ReferralTreeListDto {
    private List<ReferralTreeMemberDto> members;
    private Integer total;
    private Integer limit;
    private Integer offset;
}

