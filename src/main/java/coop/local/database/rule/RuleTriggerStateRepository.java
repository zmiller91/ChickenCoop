package coop.local.database.rule;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "piTransactionManager")
public class RuleTriggerStateRepository extends BaseRepository {

    public RuleTriggerState findByRuleId(String ruleId) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                    SELECT rts
                    FROM RuleTriggerState rts
                    WHERE rts.ruleId = :ruleId
                """, RuleTriggerState.class)
                .setParameter("ruleId", ruleId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void upsert(String ruleId, TriggerState state) {

        int updated = sessionFactory.getCurrentSession().createQuery("""
                update RuleTriggerState r
                   set r.triggerState = :state,
                       r.triggerStateDt = :now
                 where r.ruleId = :ruleId
            """).setParameter("state", state)
                .setParameter("now", System.currentTimeMillis())
                .setParameter("ruleId", ruleId)
                .executeUpdate();

        if (updated == 0) {
            RuleTriggerState triggerState = new RuleTriggerState();
            triggerState.setRuleId(ruleId);
            triggerState.setTriggerState(state);
            triggerState.setTriggerStateDt(System.currentTimeMillis());
            sessionFactory.getCurrentSession().persist(triggerState);
        }
    }


}
