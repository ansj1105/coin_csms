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
public class ReferralTransactionHistoryListDto {
    private List<ReferralTransactionHistoryDto> transactions;
    private Integer total;
    private Integer limit;
    private Integer offset;
}

