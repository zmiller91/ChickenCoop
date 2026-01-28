package coop.local.database.rule;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
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

}
