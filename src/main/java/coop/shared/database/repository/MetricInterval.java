package coop.shared.database.repository;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.function.Function;

public enum MetricInterval {

    DAY("HOUR", Duration.ofDays(1), str -> {
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
        DateTimeFormatter toFormat = DateTimeFormatter.ISO_DATE_TIME;
        return LocalDateTime.parse(str, fromFormat).format(toFormat);
    }),

    WEEK("QUARTER_DAY", Duration.ofDays(7), str -> {
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
        DateTimeFormatter toFormat = DateTimeFormatter.ISO_DATE_TIME;
        return LocalDateTime.parse(str, fromFormat).format(toFormat);
    }),

    MONTH("DAY", Duration.ofDays(31), str -> {
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter toFormat = DateTimeFormatter.ISO_DATE;
        return LocalDate.parse(str, fromFormat).format(toFormat);
    }),

    YEAR("WEEK", Duration.ofDays(366), str -> {
        DateTimeFormatter toFormat = DateTimeFormatter.ISO_DATE;
        DateTimeFormatter fromFormat = new DateTimeFormatterBuilder()
                .appendValue(WeekFields.ISO.weekBasedYear(), 4)
                .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
                .parseDefaulting(WeekFields.ISO.dayOfWeek(), DayOfWeek.SUNDAY.getValue())
                .toFormatter();

        return LocalDate.parse(str, fromFormat).format(toFormat);
    });

    private String groupingColumn;
    private Duration duration;
    private Function<String, String> dateFormatter;

    MetricInterval(String groupingColumn, Duration duration, Function<String, String> dateFormatter) {
        this.groupingColumn = groupingColumn;
        this.duration = duration;
        this.dateFormatter = dateFormatter;
    }

    public long periodBeginEpoch() {
        return System.currentTimeMillis() - duration.toMillis();
    }

    public String groupingColumn() {
        return this.groupingColumn;
    }

    public String formatDate(String dateStr) {
        return dateFormatter.apply(dateStr);
    }
}
