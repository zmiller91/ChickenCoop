package coop.shared.database.repository;

import coop.shared.database.table.rule.Rule;
import coop.shared.database.table.rule.RuleExecutionLogEntry;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class RuleExecutionLogRepository extends AuthorizerScopedRepository<RuleExecutionLogEntry> {

    @Override
    protected Class<RuleExecutionLogEntry> getObjClass() {
        return RuleExecutionLogEntry.class;
    }

    public List<RuleExecutionLogEntry> findRecent(Rule rule, int limit) {
        return this.query(
                "FROM RuleExecutionLogEntry WHERE rule = :rule ORDER BY createdAt DESC",
                RuleExecutionLogEntry.class)
                .setParameter("rule", rule)
                .setMaxResults(limit)
                .list();
    }
}
