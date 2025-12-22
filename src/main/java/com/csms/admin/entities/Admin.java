package com.csms.admin.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    private Long id;
    private String loginId;
    private String passwordHash;
    private Integer role; // 2=어드민, 3=슈퍼어드민
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

