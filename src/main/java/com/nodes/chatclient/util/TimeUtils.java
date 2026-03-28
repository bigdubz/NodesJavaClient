package com.nodes.chatclient.util;

import com.nodes.chatclient.store.model.ChatMessageUi;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private TimeUtils() {}

    public static String longToFormatted(long epochMillis, boolean specific) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        ZoneId zone = ZoneId.systemDefault();

        LocalDateTime time = instant.atZone(zone).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(zone);

        String timeStr = time.format(TIME);
        String dateStr = time.format(DATE);

        Duration diff = Duration.between(time, now);

        boolean isToday = diff.toHours() < 24 && time.toLocalDate().equals(now.toLocalDate());

        if (diff.toSeconds() < 60 && !specific) {
            return "Now";
        }

        if (diff.toMinutes() < 60 && !specific) {
            return diff.toMinutes() + "m";
        }

        if (isToday) {
            return timeStr;
        }

        if (time.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            String formatted = "Yesterday";
            if (specific)
                formatted += " at " + timeStr;
            return formatted;
        }

        String formatted = dateStr;
        if (specific)
            formatted += " at " + timeStr;
        return formatted;
    }

    public static String getDate(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime time = instant.atZone(zone).toLocalDateTime();
        return time.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    public static boolean getShouldBundle(ChatMessageUi mOld, ChatMessageUi mNew) {
        // if from the same user and within one minute, bundle
        return mOld.fromUserId.equals(mNew.fromUserId) && mNew.createdAt - mOld.createdAt < 60000;
    }

    public static boolean getShouldShowDateSeparator(ChatMessageUi mOld, ChatMessageUi mNew) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instantOld = Instant.ofEpochMilli(mOld.createdAt);
        Instant instantNew = Instant.ofEpochMilli(mNew.createdAt);

        LocalDateTime timeOld = instantOld.atZone(zone).toLocalDateTime();
        LocalDateTime timeNew = instantNew.atZone(zone).toLocalDateTime();

        return !timeOld.toLocalDate().equals(timeNew.toLocalDate());
    }
}
