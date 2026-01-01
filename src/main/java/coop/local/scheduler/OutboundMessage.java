package coop.local.scheduler;

import coop.local.database.downlink.Downlink;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class OutboundMessage {

    private Object context;
    private Downlink downlink;

    public OutboundMessage(Downlink downlink) {
        this.downlink = downlink;
    }

    private Consumer<OutboundMessage> onTxSuccess;
    private Consumer<OutboundMessage> onTxFailure;
    private Consumer<OutboundMessage> onAckSuccess;
    private Consumer<OutboundMessage> onAckIgnored;
    private Consumer<OutboundMessage> onAckFailure;

}
