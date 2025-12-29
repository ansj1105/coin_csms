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
public class TransactionHistoryListDto {
    private List<TransactionHistoryDto> transactions;
    private Integer total;
    private Integer limit;
    private Integer offset;
    private BigDecimal totalPaymentDepositAmount;
    private BigDecimal totalTokenDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalExchangeAmount;
    private BigDecimal totalSwapAmount;
    private BigDecimal totalFeeRevenue;
}

