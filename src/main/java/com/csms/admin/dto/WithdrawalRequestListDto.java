package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestListDto {
    private List<WithdrawalRequestDto> requests;
    private Integer total;
    private Integer limit;
    private Integer offset;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalFeeRevenue;
}

