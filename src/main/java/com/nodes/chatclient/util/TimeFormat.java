package com.nodes.chatclient.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private TimeFormat() {}

    public static String conversationTime(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        ZoneId zone = ZoneId.systemDefault();

        LocalDateTime time = instant.atZone(zone).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(zone);

        Duration diff = Duration.between(time, now);

        if (diff.toSeconds() < 60) {
            return "Now";
        }

        if (diff.toMinutes() < 60) {
            return diff.toMinutes() + "m";
        }

        if (diff.toHours() < 24 && time.toLocalDate().equals(now.toLocalDate())) {
            return time.format(TIME);
        }

        if (time.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "Yesterday";
        }

        return time.format(DATE);
    }
}
