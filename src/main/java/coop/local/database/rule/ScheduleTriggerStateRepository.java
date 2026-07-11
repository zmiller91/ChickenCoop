package coop.local.database.rule;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "piTransactionManager")
public class ScheduleTriggerStateRepository extends BaseRepository {

    public ScheduleTriggerState findById(String scheduleTriggerId) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                    SELECT sts
                    FROM ScheduleTriggerState sts
                    WHERE sts.scheduleTriggerId = :id
                """, ScheduleTriggerState.class)
                .setParameter("id", scheduleTriggerId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void upsert(String scheduleTriggerId, long now) {

        int updated = sessionFactory.getCurrentSession().createQuery("""
                update ScheduleTriggerState s
                   set s.lastFiredDt = :now
                 where s.scheduleTriggerId = :id
            """).setParameter("now", now)
                .setParameter("id", scheduleTriggerId)
                .executeUpdate();

        if (updated == 0) {
            ScheduleTriggerState state = new ScheduleTriggerState();
            state.setScheduleTriggerId(scheduleTriggerId);
            state.setLastFiredDt(now);
            sessionFactory.getCurrentSession().persist(state);
        }
    }
}
