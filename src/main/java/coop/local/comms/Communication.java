package coop.local.comms;

import coop.local.comms.message.*;
import coop.local.comms.serial.SerialCommunication;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@Log4j2
public class Communication implements Runnable {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final SerialCommunication serial;
    private final List<Byte> buffer;
    private boolean stopped = false;
    private Thread readingThread;
    private final Map<Class<? extends PiMessage>, List<Consumer<? extends PiMessage>>> listeners;

    public Communication(SerialCommunication serial) {
        this.serial = serial;
        this.buffer = new ArrayList<>();
        this.listeners = new HashMap<>();
    }

    /**
     * Read incoming data from the serial device. If we run across a newline character then we will flush the
     * buffer to the listeners and reset the buffer.
     */
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

    /**
     * Starts listening for incoming communication on a separate thread.
     */
    public void beginReading() {
        if(readingThread == null) {
            readingThread = new Thread(this);
            readingThread.setDaemon(true);
            readingThread.start();
        }
    }

    /**
     * Sends a message to the serial device and waits for either success or error ack back. This method is not thread
     * safe and is therefore synchornized. There is no way to associate the sending of a message to the success or
     * error status returned by the LoRa module, so we assume that after sending the message the next error or success
     * message is related to the message we just sent. We will eventually timeout if a success or error message isn't
     * received.
     *
     * @param address
     * @param message
     * @return
     */
    public synchronized MessageSent write(int address, String message) {

        MessageSent sent = new MessageSent(address, message);

        Consumer<MessageError> errorConsumer = sent::setError;
        Consumer<MessageSuccess> successConsumer = sent::setSuccess;

        addListener(MessageError.class, errorConsumer);
        addListener(MessageSuccess.class, successConsumer);

        serial.write(sent.serialize());

        long start = System.currentTimeMillis();
        while(sent.inProgress()) {
            if(System.currentTimeMillis() - start >= TIMEOUT.toMillis()) {
                sent.setTimedOut(true);
            }

            sleep(100);
        }

        removeListener(MessageError.class, errorConsumer);
        removeListener(MessageSuccess.class, successConsumer);

        return sent;
    }

    /**
     * Add a listener for a specific message type.
     *
     * @param type
     * @param listener
     * @param <T>
     */
    public <T extends PiMessage> void addListener(Class<T> type, Consumer<T> listener) {
        List<Consumer<? extends PiMessage>> listenersForType = this.listeners.getOrDefault(type, new ArrayList<>());
        listenersForType.add(listener);
        this.listeners.put(type, listenersForType);
    }

    /**
     * Removes a listener for a specific message type.
     *
     * @param type
     * @param listener
     * @param <T>
     */
    public <T extends PiMessage> void removeListener(Class<T> type, Consumer<T> listener) {
        List<Consumer<? extends PiMessage>> listenersForType = this.listeners.getOrDefault(type, new ArrayList<>());
        listenersForType.remove(listener);
        this.listeners.put(type, listenersForType);
    }

    public void stop() {
        this.stopped = true;
    }

    /**
     * Convert the byte buffer into a string and call the listeners.
     */
    private void flush() {
        if (!buffer.isEmpty()) {

            byte[] bytes = new byte[buffer.size()];
            for(int i = 0; i < buffer.size(); i++) {
                bytes[i] = buffer.get(i);
            }

            buffer.clear();

            try {
                invokeListeners(new String(bytes));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception from listener: " + e.getMessage());
            }
        }
    }

    private void invokeListeners(String input) {

        log.info("Recieved: " + input);

        if (MessageError.matches(input)) {
            this.listeners.getOrDefault(MessageError.class, new ArrayList<>())
                    .forEach(l -> ((Consumer<MessageError>)l).accept(new MessageError(input)));
        }

        if (MessageReceived.matches(input)) {
            this.listeners.getOrDefault(MessageReceived.class, new ArrayList<>())
                    .forEach(l -> ((Consumer<MessageReceived>)l).accept(new MessageReceived(input)));
        }

        if(MessageSuccess.matches(input)) {
            this.listeners.getOrDefault(MessageSuccess.class, new ArrayList<>())
                    .forEach(l -> ((Consumer<MessageSuccess>)l).accept(new MessageSuccess(input)));
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
