package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.rule.Rule;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class RuleRepository extends AuthorizerScopedRepository<Rule> {

    @Override
    protected Class<Rule> getObjClass() {
        return Rule.class;
    }

    public List<Rule> findByCoop(Coop coop) {
        return this.query("FROM Rule WHERE coop = :coop", Rule.class)
                .setParameter("coop", coop)
                .list();
    }

    public Rule findByCoopAndId(Coop coop, String id) {
        return this.query("FROM Rule WHERE coop = :coop AND id = :id", Rule.class)
                .setParameter("coop", coop)
                .setParameter("id", id)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }
}
