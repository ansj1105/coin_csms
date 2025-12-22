package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanctionRequestDto {
    private String sanctionStatus; // WARNING, MINING_SUSPENDED, ACCOUNT_BLOCKED, null
}

