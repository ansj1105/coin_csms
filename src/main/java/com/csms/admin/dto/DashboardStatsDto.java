package com.csms.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsDto {
    private StatsDto stats;
    private List<ChartDataDto> chartData;
    private List<TopMemberDto> topMembers;
    
    @Data
    @Builder
    public static class StatsDto {
        private Double totalIssuance;
        private Double totalMined;
        private Integer totalDeposit;
        private Integer totalPayment;
        private Integer totalExchange;
        private Integer totalWithdrawal;
        private Double totalMiningAmount;
        private Double totalPaymentAmount;
        private Double totalExchangeAmount;
        private Double totalWithdrawalAmount;
        private Double totalWithdrawalFeeRevenue;
        private Integer coinDepositCount;
        private Integer paymentDepositCount;
        private Integer swapCount;
        private Integer exchangeCount;
        private Integer withdrawalCount;
        private Integer realtimeMiningUsers;
        private Integer realtimeBroadcasts;
        private Integer realtimeListeners;
        private Integer referralRegistrationCount;
        private Integer totalUsers;
    }
    
    @Data
    @Builder
    public static class ChartDataDto {
        private String date;
        private Double totalMined;
        private Integer userCount;
    }
    
    @Data
    @Builder
    public static class TopMemberDto {
        private Integer rank;
        private String nickname;
        private Double minedAmount;
    }
}

