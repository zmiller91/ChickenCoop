package coop.local.comms.serial;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.StopBits;

import static java.lang.Thread.sleep;

public class PiSerialCommunication implements SerialCommunication {

    private final Serial serial;

    public PiSerialCommunication(String device) {

        Context pi4j = Pi4J.newAutoContext();
        serial = pi4j.create(Serial.newConfigBuilder(pi4j)
                .use_115200_N81()
                .dataBits_8()
                .parity(Parity.NONE)
                .stopBits(StopBits._1)
                .flowControl(FlowControl.NONE)
                .device(device)
                .id("serial")
                .provider("pigpio-serial")
                .build());

        serial.open();
        while(!serial.isOpen()) {
            try {sleep(100);} catch (InterruptedException e) {throw new RuntimeException(e);}
        }
    }

    @Override
    public boolean bytesAvailable() {
        return serial.available() > 0;
    }

    @Override
    public byte[] readBytes() {

        int available = serial.available();
        byte[] buffer = new byte[available];
        int read = serial.read(buffer, available);

        // TODO: read < available

        return buffer;
    }

    @Override
    public int write(byte[] bytes) {
        return serial.write(bytes);
    }
}
