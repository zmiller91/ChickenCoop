package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.CoopMetric;
import lombok.Data;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@EnableTransactionManagement
@Transactional
public class MetricRepository extends AuthorizerScopedRepository<CoopMetric> {
    @Override
    protected Class<CoopMetric> getObjClass() {
        return CoopMetric.class;
    }

    public List<MetricData> findByMetricHourly(Coop coop, String metric) {
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd ha");

        return findByMetric(coop, metric, "HOUR").stream().map(d -> {
            LocalDateTime zdt = LocalDateTime.parse(d.getDate(), fromFormat);
            d.setDate(zdt.format(toFormat));
            return d;

        }).collect(Collectors.toList());
    }

    public List<MetricData> findByMetricDaily(Coop coop, String metric) {
        List<MetricData> data = findByMetric(coop, metric, "DAY");
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd");
        return formatLocalDate(data, fromFormat, toFormat);
    }

    public List<MetricData> findByMetricWeekly(Coop coop, String metric) {

        List<MetricData> data = findByMetric(coop, metric, "WEEK");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd");
        DateTimeFormatter fromFormat = new DateTimeFormatterBuilder()
                .appendValue(WeekFields.ISO.weekBasedYear(), 4)
                .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
                .parseDefaulting(WeekFields.ISO.dayOfWeek(), DayOfWeek.SUNDAY.getValue())
                .toFormatter();

        return formatLocalDate(data, fromFormat, toFormat);
    }

    public List<MetricData> findByMetricMonthly(Coop coop, String metric) {

        List<MetricData> data = findByMetric(coop, metric, "MONTH");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        DateTimeFormatter fromFormat = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, DayOfWeek.SATURDAY.getValue())
                .toFormatter();

        return formatLocalDate(data, fromFormat, toFormat);
    }

    private List<MetricData> findByMetric(
            Coop coop,
            String metric,
            String column) {

        String query = String.format("""
                SELECT
                    CAST(`%s` AS CHAR) AS `date`,
                    AVG(`VALUE`) AS `value`
                FROM metrics
                WHERE COOP_ID = :coopId
                AND metric = :metric
                GROUP BY `%s`
        """, column, column);

        return sessionFactory.getCurrentSession().createNativeQuery(query)
                .setParameter("coopId", coop.getId())
                .setParameter("metric", metric)
                .setResultTransformer(new AliasToBeanResultTransformer(MetricData.class))
                .list();
    }

    public List<MetricData> formatLocalDate(
            List<MetricData> data,
            DateTimeFormatter fromFormat,
            DateTimeFormatter toFormat) {

        return data.stream().peek(d -> {
            LocalDate zdt = LocalDate.parse(d.getDate(), fromFormat);
            d.setDate(zdt.format(toFormat));
        }).collect(Collectors.toList());
    }

    public CoopMetric save(Coop coop, String componentId, long date, String name, Double value) {

        Instant instant = Instant.ofEpochMilli(date);
        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/Chicago"));
        DateTimeFormatter yearFormat = DateTimeFormatter.ofPattern("yyyy");
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter weekFormat = DateTimeFormatter.ofPattern("yyyyww");
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("yyyyMMddhh");

        CoopMetric coopMetric = new CoopMetric();

        coopMetric.setDt(date);
        coopMetric.setYear(Integer.parseInt(yearFormat.format(zdt)));
        coopMetric.setMonth(Integer.parseInt(monthFormat.format(zdt)));
        coopMetric.setWeek(Integer.parseInt(weekFormat.format(zdt)));
        coopMetric.setDay(Integer.parseInt(dayFormat.format(zdt)));
        coopMetric.setHour(Integer.parseInt(hourFormat.format(zdt)));

        coopMetric.setCoop(coop);
        coopMetric.setComponentId(componentId);
        coopMetric.setMetric(name);
        coopMetric.setValue(value);

        this.persist(coopMetric);
        return coopMetric;
    }


    @Data
    public static class MetricData {
        private String date;
        private Double value;
    }
}