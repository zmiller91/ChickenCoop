package coop.local.database.downlink;

import coop.device.protocol.DownlinkFrame;
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "downlink")
@Data
public class Downlink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frame_id", nullable = false, length = 64)
    private String frameId;

    @Column(name = "serial_number", nullable = false, length = 64)
    private String serialNumber;

    @Column(name="frame")
    private String frame;

    @Column(name = "requires_ack", nullable = false)
    private boolean requiresAck;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "sent_at")
    private Long sentAt;

    @Column(name = "ack_at")
    private Long ackAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = System.currentTimeMillis();
    }

    public static Downlink from(DownlinkFrame frame) {
        Downlink downlink = new Downlink();
        downlink.setFrameId(frame.getId());
        downlink.setSerialNumber(frame.getSerialNumber());
        downlink.setFrame(frame.toString());
        downlink.setRequiresAck(frame.getRequiresAck());
        return downlink;
    }
}