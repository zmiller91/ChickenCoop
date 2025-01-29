package coop.local.state;

import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.metric.Metric;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Getter
public abstract class LocalStateProvider extends StateProvider {

    private CoopState config = null;

    public abstract void init();
    public abstract void refreshState();
    public abstract void save(Metric metric);

    @Override
    public void put(CoopState coopState) {
        this.config = coopState;
    }
}
