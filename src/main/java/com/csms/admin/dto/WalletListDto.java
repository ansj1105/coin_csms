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
public class WalletListDto {
    private List<WalletInfo> wallets;
    private Long total;
    private Integer limit;
    private Integer offset;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletInfo {
        private Long id;
        private String network;
        private String token;
        private String address;
        private Double balance;
    }
}

