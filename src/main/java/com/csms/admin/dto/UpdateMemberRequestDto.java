package com.csms.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequestDto {
    private String phone;
    // email 컬럼이 users 테이블에 없으므로 제거
    // private String email;
    private Integer level;
}

