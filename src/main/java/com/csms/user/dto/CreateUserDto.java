package com.csms.user.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CreateUserDto {
    private String loginId;
    private String password;
    private String passwordHash;
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("login_id", loginId);
        map.put("password_hash", passwordHash);
        return map;
    }
}

