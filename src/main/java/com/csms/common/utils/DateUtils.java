package com.csms.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {
    
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }
    
    public static LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }
    
    public static DateRange calculateDateRange(String dateRange, String startDate, String endDate) {
        LocalDate end = LocalDate.now();
        LocalDate start;
        
        if ("custom".equals(dateRange) && startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            int days = switch (dateRange != null ? dateRange : "7") {
                case "14" -> 14;
                case "30" -> 30;
                case "180" -> 180;
                case "365" -> 365;
                default -> 7;
            };
            start = end.minusDays(days);
        }
        
        // 최대 365일 제한
        if (start.isBefore(end.minusDays(365))) {
            start = end.minusDays(365);
        }
        
        return new DateRange(start, end);
    }
    
    public record DateRange(LocalDate startDate, LocalDate endDate) {}
}

