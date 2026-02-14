package coop.local;

import coop.device.protocol.event.Event;
import coop.device.protocol.event.MetricEvent;
import coop.local.database.metric.MetricCacheEntry;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.listener.EventListener;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.events.MetricReceived;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class MetricProcessor implements EventListener {

    private final MetricCacheRepository metricCache;
    private final LocalStateProvider provider;

    public MetricProcessor(MetricCacheRepository metricCache, LocalStateProvider provider) {
        this.metricCache = metricCache;
        this.provider = provider;
    }

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(MetricEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        if(payload.getEvent() instanceof MetricEvent event) {
            processMetricEvent(payload, event);
        }
    }

    private void processMetricEvent(EventPayload message, MetricEvent event) {

        MetricReceived metric = new MetricReceived();
        metric.setDt(System.currentTimeMillis());
        metric.setCoopId(message.getCoop().getCoopId());
        metric.setComponentId(message.getComponent().getComponentId());
        metric.setMetric(event.getMetric());
        metric.setValue(event.getValue());

        provider.save(metric);

        MetricCacheEntry metricCacheEntry = new MetricCacheEntry();
        metricCacheEntry.setMetric(event.getMetric());
        metricCacheEntry.setValue(event.getValue());
        metricCacheEntry.setComponentId(message.getComponent().getComponentId());
        metricCache.upsert(metricCacheEntry);
    }
}
