package coop.local.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.StopBits;
import com.pi4j.util.Console;

public class Test {

    public static void main(String... args) throws Exception {
        Context pi4j = Pi4J.newAutoContext();
        Serial serial = pi4j.create(Serial.newConfigBuilder(pi4j)
                .use_9600_N81()
                .dataBits_8()
                .parity(Parity.NONE)
                .stopBits(StopBits._1)
                .flowControl(FlowControl.NONE)
                .id("serial")
                .provider("pigpio-serial")
                .build());

        while(!serial.isOpen()) {
            Thread.sleep(250);
        }

        // Start a thread to handle the incoming data from the serial port
        SerialReader serialReader = new SerialReader(new Console(), serial);
        Thread serialReaderThread = new Thread(serialReader, "SerialReader");
        serialReaderThread.setDaemon(true);
        serialReaderThread.start();

        while (serial.isOpen()) {
            Thread.sleep(500);
        }

        serialReader.stopReading();



    }

}
