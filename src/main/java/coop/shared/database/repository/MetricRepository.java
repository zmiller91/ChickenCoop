package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.CoopMetric;
import lombok.Data;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class MetricRepository extends AuthorizerScopedRepository<CoopMetric> {
    @Override
    protected Class<CoopMetric> getObjClass() {
        return CoopMetric.class;
    }

    public List<MetricData> findByMetric(Coop coop, String metric) {
        return sessionFactory.getCurrentSession().createNativeQuery(
                """
                SELECT
                    CAST(`WEEK` AS CHAR) AS `date`,
                    AVG(`VALUE`) AS `value`
                FROM metrics
                WHERE COOP_ID = :coopId
                AND metric = :metric
                GROUP BY `WEEK`
                """)
                .setParameter("coopId", coop.getId())
                .setParameter("metric", metric)
                .setResultTransformer(new AliasToBeanResultTransformer(MetricData.class))
                .list();
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