package com.csms.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponseDto {
    private String accessToken;
    private String adminId;
}

