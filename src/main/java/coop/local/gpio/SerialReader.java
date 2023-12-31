package coop.local.gpio;

import com.pi4j.io.serial.Serial;
import com.pi4j.util.Console;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class SerialReader implements Runnable {

    private final Console console;
    private final Serial serial;
    private final Consumer<String> callback;

    private boolean continueReading = true;

    public SerialReader(Console console, Serial serial, Consumer<String> callback) {
        this.console = console;
        this.serial = serial;
        this.callback = callback;
    }

    public void stopReading() {
        continueReading = false;
    }

    @Override
    public void run() {
        // We use a buffered reader to handle the data received from the serial port
        BufferedReader br = new BufferedReader(new InputStreamReader(serial.getInputStream()));

        try {
            // Data from the GPS is recieved in lines
            String line = "";

            // Read data until the flag is false
            while (continueReading) {
                // First we need to check if there is data available to read.
                // The read() command for pigio-serial is a NON-BLOCKING call,
                // in contrast to typical java input streams.
                var available = serial.available();
                if (available > 0) {
                    for (int i = 0; i < available; i++) {
                        byte b = (byte) br.read();
                        if (b < 32) {
                            // All non-string bytes are handled as line breaks
                            if (!line.isEmpty()) {
                                callback.accept(line);
                                line = "";
                            }
                        } else {
                            line += (char) b;
                        }
                    }
                } else {
                    Thread.sleep(10);
                }
            }
        } catch (Exception e) {
            console.println("Error reading data from serial: " + e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }
}