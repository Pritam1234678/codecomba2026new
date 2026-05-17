package com.example.codecombat2026.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtil {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Get current time in India/Kolkata timezone
     * 
     * @return LocalDateTime in Asia/Kolkata timezone
     */
    public static LocalDateTime now() {
        return ZonedDateTime.now(INDIA_ZONE).toLocalDateTime();
    }

    /**
     * Get ZoneId for India/Kolkata
     * 
     * @return ZoneId for Asia/Kolkata
     */
    public static ZoneId getIndiaZone() {
        return INDIA_ZONE;
    }
}
