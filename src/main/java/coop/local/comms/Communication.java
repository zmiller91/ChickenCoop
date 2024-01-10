package coop.local.comms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Communication implements Runnable {

    private final SerialCommunication serial;
    private final List<Byte> buffer;
    private boolean stopped = false;
    private Thread readingThread;
    private final List<Consumer<byte[]>> listeners;

    public Communication(SerialCommunication serial) {
        this.serial = serial;
        this.buffer = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    @Override
    public void run() {
        while(!stopped) {

            while (!stopped && !serial.bytesAvailable()) {
                sleep(250);
            }

            if(!stopped) {
                byte[] readBuffer = serial.readBytes();
                for (byte b : readBuffer) {
                    if (b == '\r' || b == '\n') {
                        flush();
                    }
                    else {
                        buffer.add(b);
                    }
                }
            }
        }
    }

    public void beginReading() {
        if(readingThread == null) {
            readingThread = new Thread(this);
            readingThread.setDaemon(true);
            readingThread.start();
        }
    }

    public void write(int address, String message) {
        String toWrite = String.format("AT+SEND=%s,%s,%s\r\n", address, message.length(), message);
        serial.write(toWrite.getBytes());
    }

    public void addListener(Consumer<byte[]> listener) {
        this.listeners.add(listener);
    }

    public void stop() {
        this.stopped = true;
    }

    private void flush() {
        if (!buffer.isEmpty()) {

            byte[] bytes = new byte[buffer.size()];
            for(int i = 0; i < buffer.size(); i++) {
                bytes[i] = buffer.get(i);
            }

            buffer.clear();
            this.listeners.forEach(l -> l.accept(bytes));
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
