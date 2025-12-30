package coop.device.protocol.command;

import coop.device.protocol.DownlinkFrame;

public class EndCommandDownlink extends DownlinkFrame {
    public EndCommandDownlink(String serialNumber) {
        super(serialNumber, "END");
        setRequiresAck(false);
    }
}
