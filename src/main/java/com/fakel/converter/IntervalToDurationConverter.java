package com.fakel.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

@Converter(autoApply = true)
public class IntervalToDurationConverter implements AttributeConverter<Duration, String> {

    @Override
    public String convertToDatabaseColumn(Duration duration) {
        if (duration == null) return null;
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%d hours %d minutes %d seconds", hours, minutes, secs);
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        try {
            // Пробуем распарсить формат PostgreSQL interval
            String[] parts = dbData.split(" ");
            long hours = 0, minutes = 0, seconds = 0;

            for (int i = 0; i < parts.length; i++) {
                if (i + 1 < parts.length) {
                    switch (parts[i + 1]) {
                        case "hours":
                        case "hour":
                            hours = Long.parseLong(parts[i]);
                            break;
                        case "minutes":
                        case "minute":
                            minutes = Long.parseLong(parts[i]);
                            break;
                        case "seconds":
                        case "second":
                            seconds = Long.parseLong(parts[i]);
                            break;
                    }
                }
            }

            return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        } catch (Exception e) {
            // Если не получилось распарсить, возвращаем Duration.ZERO
            return Duration.ZERO;
        }
    }
}