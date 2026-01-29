package com.csms.user.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String loginId;
    private String passwordHash;
    private String email;
    private String nickname;  // 표시용 닉네임 (2~8자리)
    private String name;  // 실제 사용자 이름 (1~50자, display_name에서 변경됨)
    private String gender;  // M/F/O 또는 NULL
    private String phone;
    private String referralCode;
    private Integer role;  // 0: 테스트, 1: 유저, 2: 어드민, 3: 슈퍼어드민
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // soft delete
}

