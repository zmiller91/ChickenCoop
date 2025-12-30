package coop.shared.database.table.component;

import coop.device.DeviceType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "component_serials")
public class ComponentSerial {

    @Id
    @Column(name = "SERIAL_NUMBER")
    private String serialNumber;

    @Column(name="TYPE")
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @OneToOne
    @JoinColumn(name="SERIAL_NUMBER")
    private Component component;
}
