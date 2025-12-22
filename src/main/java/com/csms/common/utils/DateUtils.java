package com.csms.common.utils;

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
}

