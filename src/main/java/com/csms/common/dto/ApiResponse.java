package com.csms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .status("OK")
            .message("요청이 완료되었습니다.")
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status("OK")
            .message(message)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
            .status("FAIL")
            .message(message)
            .data(null)
            .build();
    }
}

