package coop.remote.schedule;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.PortActionLogRepository;
import coop.shared.database.table.Pi;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.PortActionLogEntry;
import coop.shared.database.table.component.PortActionSource;
import coop.shared.database.table.component.PortActionStatus;
import coop.shared.pi.events.PortActionHubEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class PortActionProcessor {

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private PortActionLogRepository portActionLogRepository;

    @Transactional
    public void process(Pi pi, PortActionHubEvent event) {

        Component component = componentRepository.findById(pi, event.getComponentId());
        if(component == null) {
            log.warn("Could not find component " + event.getComponentId() + " for pi " + pi.getId());
            return;
        }

        // A device's STATE broadcast reports every port unconditionally (on-change and every 30 minutes for
        // self-healing) - skip persisting a COMPLETE entry that just repeats the port's last known state, so
        // periodic self-healing broadcasts don't spam the log with redundant entries for ports that didn't change.
        if("COMPLETE".equals(event.getStatus()) && matchesLastKnownState(component, event)) {
            return;
        }

        PortActionLogEntry entry = new PortActionLogEntry();
        entry.setComponent(component);
        entry.setPortIndex(event.getPortIndex());
        entry.setActionKey(event.getActionKey());
        entry.setSource(event.getSource() != null ? PortActionSource.valueOf(event.getSource()) : null);
        entry.setStatus(PortActionStatus.valueOf(event.getStatus()));
        entry.setCreatedAt(event.getDt());
        portActionLogRepository.persist(entry);
    }

    private boolean matchesLastKnownState(Component component, PortActionHubEvent event) {
        return portActionLogRepository.findLatestComplete(component).stream()
                .anyMatch(e -> e.getPortIndex() == event.getPortIndex() && e.getActionKey().equals(event.getActionKey()));
    }
}
