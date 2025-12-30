package coop.device.protocol;

public class EndDownlink extends DownlinkFrame {
    public EndDownlink(String serialNumber) {
        super(serialNumber, "END");
        setRequiresAck(false);
    }
}
