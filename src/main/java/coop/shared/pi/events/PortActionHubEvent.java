package coop.shared.pi.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PortActionHubEvent extends HubEvent {

    private String componentId;
    private int portIndex;
    private String actionKey;

    /** MANUAL or RULE - only meaningful for REQUESTED entries; null for COMPLETE/FAILED/CANCELLED, since
     * completion always flows through the same Scheduler regardless of what originally requested it. */
    private String source;

    /** REQUESTED, COMPLETE, FAILED, or CANCELLED - mirrors coop.shared.database.table.component.PortActionStatus. */
    private String status;

    public PortActionHubEvent(String componentId, int portIndex, String actionKey, String source, String status) {
        this.componentId = componentId;
        this.portIndex = portIndex;
        this.actionKey = actionKey;
        this.source = source;
        this.status = status;
    }

    @Override
    public HubEventType getType() {
        return HubEventType.PORT_ACTION;
    }
}
