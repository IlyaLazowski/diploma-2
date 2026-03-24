package com.fakel.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Converter(autoApply = true)
public class IntervalToDurationConverter implements AttributeConverter<Duration, String> {

    private static final Pattern INTERVAL_PATTERN = Pattern.compile(
            "(?:(\\d+) days?)?\\s*" +
                    "(?:(\\d+) hours?)?\\s*" +
                    "(?:(\\d+) minutes?)?\\s*" +
                    "(?:(\\d+(?:\\.\\d+)?) seconds?)?"
    );

    @Override
    public String convertToDatabaseColumn(Duration duration) {
        if (duration == null) return null;

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        double seconds = duration.getSeconds() % 60 + duration.getNano() / 1_000_000_000.0;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" days ");
        if (hours > 0) sb.append(hours).append(" hours ");
        if (minutes > 0) sb.append(minutes).append(" minutes ");
        if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            // Форматируем секунды без лишних нулей
            if (seconds == (long) seconds) {
                sb.append((long) seconds).append(" seconds");
            } else {
                sb.append(String.format("%.3f", seconds)).append(" seconds");
            }
        }

        return sb.toString().trim();
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) return null;

        try {
            // Пробуем стандартный парсинг PostgreSQL interval
            if (dbData.contains(":")) {
                // Формат HH:MM:SS или MM:SS
                return parseTimeFormat(dbData);
            }

            Matcher matcher = INTERVAL_PATTERN.matcher(dbData);
            if (matcher.find()) {
                long days = parseGroup(matcher.group(1));
                long hours = parseGroup(matcher.group(2));
                long minutes = parseGroup(matcher.group(3));
                double seconds = parseDoubleGroup(matcher.group(4));

                return Duration.ofDays(days)
                        .plusHours(hours)
                        .plusMinutes(minutes)
                        .plus((long) (seconds * 1_000_000_000), ChronoUnit.NANOS);
            }

            // Если ничего не распарсилось, пробуем как просто секунды
            try {
                double seconds = Double.parseDouble(dbData.trim());
                return Duration.ofMillis((long) (seconds * 1000));
            } catch (NumberFormatException e) {
                // Игнорируем
            }

            return Duration.ZERO;
        } catch (Exception e) {
            System.err.println("Ошибка парсинга interval: " + dbData);
            e.printStackTrace();
            return Duration.ZERO;
        }
    }

    private Duration parseTimeFormat(String timeStr) {
        String[] parts = timeStr.split(":");
        double totalSeconds = 0;

        if (parts.length == 3) {
            // HH:MM:SS
            totalSeconds += Integer.parseInt(parts[0]) * 3600;
            totalSeconds += Integer.parseInt(parts[1]) * 60;
            totalSeconds += Double.parseDouble(parts[2]);
        } else if (parts.length == 2) {
            // MM:SS
            totalSeconds += Integer.parseInt(parts[0]) * 60;
            totalSeconds += Double.parseDouble(parts[1]);
        }

        return Duration.ofMillis((long) (totalSeconds * 1000));
    }

    private long parseGroup(String group) {
        if (group == null || group.isEmpty()) return 0;
        try {
            return Long.parseLong(group);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleGroup(String group) {
        if (group == null || group.isEmpty()) return 0;
        try {
            return Double.parseDouble(group);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}