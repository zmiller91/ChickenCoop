package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.CoopComponent;
import coop.shared.database.table.CoopMetric;
import lombok.*;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@EnableTransactionManagement
@Transactional
public class MetricRepository extends AuthorizerScopedRepository<CoopMetric> {

    @Override
    protected Class<CoopMetric> getObjClass() {
        return CoopMetric.class;
    }

    public List<MetricDataRow> findByMetricHourly(Coop coop, String metric) {
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd ha");

        return findByMetric(coop, metric, "HOUR").stream().map(d -> {
            LocalDateTime zdt = LocalDateTime.parse(d.getDate(), fromFormat);
            d.setDate(zdt.format(toFormat));
            return d;

        }).collect(Collectors.toList());
    }

    public List<MetricDataRow> findByMetricDaily(Coop coop, String metric) {
        List<MetricDataRow> data = findByMetric(coop, metric, "DAY");
        DateTimeFormatter fromFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd");
        return formatLocalDate(data, fromFormat, toFormat);
    }

    public List<MetricDataRow> findByMetricWeekly(Coop coop, String metric) {

        List<MetricDataRow> data = findByMetric(coop, metric, "WEEK");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM dd");
        DateTimeFormatter fromFormat = new DateTimeFormatterBuilder()
                .appendValue(WeekFields.ISO.weekBasedYear(), 4)
                .appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2)
                .parseDefaulting(WeekFields.ISO.dayOfWeek(), DayOfWeek.SUNDAY.getValue())
                .toFormatter();

        return formatLocalDate(data, fromFormat, toFormat);
    }

    public List<MetricDataRow> findByMetricMonthly(Coop coop, String metric) {

        List<MetricDataRow> data = findByMetric(coop, metric, "MONTH");
        DateTimeFormatter toFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        DateTimeFormatter fromFormat = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, DayOfWeek.SATURDAY.getValue())
                .toFormatter();

        return formatLocalDate(data, fromFormat, toFormat);
    }

    public List<ComponentData> findByCoop(Coop coop, MetricInterval interval) {

        String query = String.format("""
                SELECT
                    `COMPONENT_ID` AS `componentId`,
                    CAST(`%s` AS CHAR) AS `date`,
                    `METRIC` AS `metric`,
                    ROUND(AVG(`VALUE`)) as `value`
                FROM metrics
                WHERE `COOP_ID` = :coopId
                AND `DT` >= :dt
                GROUP BY `COMPONENT_ID`, `%s`, `METRIC`
                ORDER BY `%s` ASC;
        """, interval.groupingColumn(), interval.groupingColumn(), interval.groupingColumn());

        List<ComponentDataRow> componentDataRows = sessionFactory.getCurrentSession().createNativeQuery(query)
                .setParameter("coopId", coop.getId())
                .setParameter("dt", interval.periodBeginEpoch())
                .setResultTransformer(new AliasToBeanResultTransformer(ComponentDataRow.class))
                .list();

        Map<String, ComponentData> groupedData = new HashMap<>();
        componentDataRows.forEach(data -> {
            ComponentData componentData = groupedData.getOrDefault(data.getComponentId(), new ComponentData(data.componentId));
            componentData.add(data, interval);
            groupedData.put(data.getComponentId(), componentData);
        });

        for(ComponentData data : groupedData.values()) {
            data.setBatteryLevel(getBatteryLevel(coop, data.componentId));
            data.setLastUpdate(getLastUpdate(coop, data.getComponentId()));
        }

        return new ArrayList<>(groupedData.values());
    }

    public Long getLastUpdate(Coop coop, String component) {
        String query = """
                SELECT MAX(DT)
                FROM metrics
                WHERE COOP_ID = :coopId
                AND COMPONENT_ID = :componentId
                """;

        List<Object> result = sessionFactory.getCurrentSession().createNativeQuery(query)
                .setParameter("coopId", coop.getId())
                .setParameter("componentId", component)
                .list();

        return result.stream().map(r -> ((BigInteger) r).longValue()).findFirst().orElse(null);
    }

    public Long getLastUpdate(Coop coop, CoopComponent component) {
        return getLastUpdate(coop, component.getComponentId());
    }

    public Double getBatteryLevel(Coop coop, String component) {
        String query = """
                SELECT `VALUE`
                FROM metrics
                WHERE COOP_ID = :coopId
                AND COMPONENT_ID = :componentId
                AND METRIC = 'BATTERY'
                ORDER BY DT DESC
                LIMIT 1
                """;

        List<Object> result = sessionFactory.getCurrentSession().createNativeQuery(query)
                .setParameter("coopId", coop.getId())
                .setParameter("componentId", component)
                .list();

        return result.stream().map(r -> ((Float) r).doubleValue()).findFirst().orElse(null);
    }

    public Double getBatteryLevel(Coop coop, CoopComponent component) {
        return getBatteryLevel(coop, component.getComponentId());
    }

    public ComponentData findByCoopComponent(Coop coop, CoopComponent component, MetricInterval interval) {

        String query = String.format("""
                SELECT
                    `COMPONENT_ID` AS `componentId`,
                    CAST(`%s` AS CHAR) AS `date`,
                    `METRIC` AS `metric`,
                    ROUND(AVG(`VALUE`)) as `value`
                FROM metrics
                WHERE `COOP_ID` = :coopId
                AND `DT` >= :dt
                AND `COMPONENT_ID` = :componentId
                GROUP BY `COMPONENT_ID`, `%s`, `METRIC`
                ORDER BY `%s` ASC;
        """, interval.groupingColumn(), interval.groupingColumn(), interval.groupingColumn());

        List<ComponentDataRow> componentDataRows = sessionFactory.getCurrentSession().createNativeQuery(query)
                .setParameter("coopId", coop.getId())
                .setParameter("componentId", component.getComponentId())
                .setParameter("dt", interval.periodBeginEpoch())
                .setResultTransformer(new AliasToBeanResultTransformer(ComponentDataRow.class))
                .list();

        Map<String, ComponentData> groupedData = new HashMap<>();
        componentDataRows.forEach(row -> {
            ComponentData componentData = groupedData.getOrDefault(row.getComponentId(), new ComponentData(row.componentId));
            componentData.add(row, interval);
            groupedData.put(row.getComponentId(), componentData);
        });

        ComponentData result = groupedData.get(component.getComponentId());
        if(result == null) {
            result = new ComponentData(component.getComponentId());
        } else {

            Long lastUpdate = getLastUpdate(coop, component);
            result.setLastUpdate(lastUpdate);

            Double batteryLevel = getBatteryLevel(coop, component);
            result.setBatteryLevel(batteryLevel);
        }

        return result;
    }

    private List<MetricDataRow> findByMetric(
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
                .setResultTransformer(new AliasToBeanResultTransformer(MetricDataRow.class))
                .list();
    }

    public List<MetricDataRow> formatLocalDate(
            List<MetricDataRow> data,
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
        DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");

        CoopMetric coopMetric = new CoopMetric();

        coopMetric.setDt(date);
        coopMetric.setYear(Integer.parseInt(yearFormat.format(zdt)));
        coopMetric.setMonth(Integer.parseInt(monthFormat.format(zdt)));
        coopMetric.setWeek(Integer.parseInt(weekFormat.format(zdt)));
        coopMetric.setDay(Integer.parseInt(dayFormat.format(zdt)));

        int hour = Integer.parseInt(hourFormat.format(zdt));
        int quarterDay = (hour / 4) * 4;

        coopMetric.setQuarterDay(quarterDay);
        coopMetric.setHour(hour);

        coopMetric.setCoop(coop);
        coopMetric.setComponentId(componentId);
        coopMetric.setMetric(name);
        coopMetric.setValue(value);

        this.persist(coopMetric);
        return coopMetric;
    }

    @Data
    public static class ComponentData {

        private final String componentId;
        private Long lastUpdate;
        private Double batteryLevel;
        private List<Map<String, Object>> data;

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<String, Map<String, Object>> grouped;

        private ComponentData(String componentId) {
            this.componentId = componentId;
            this.grouped = new HashMap<>();
        }

        public List<Map<String, Object>> getData() {
            Comparator<String> natural = Comparator.naturalOrder();
            return grouped.entrySet()
                    .stream()
                    .map(entry -> {
                        entry.getValue().put("idx", entry.getKey());
                        return entry.getValue();
                    })
                    .sorted((a, b) -> natural.compare((String) a.get("idx"), (String) b.get("idx")))
                    .collect(Collectors.toList());
        }

        private void add(ComponentDataRow row, MetricInterval interval) {
            String date = row.date;
            String formatted = interval.formatDate(row.date);
            Map<String, Object> data = grouped.getOrDefault(date, new HashMap<>());

            data.put(row.metric, row.value);
            data.put("date", formatted);
            grouped.put(date, data);
        }
    }

    @Data
    public static class ComponentDataRow {
        private String componentId;
        private String date;
        private String metric;
        private Double value;
    }

    @Data
    @AllArgsConstructor
    public static class MetricDataRow {
        private String date;
        private Double value;
    }
}