package coop.local;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.StopBits;
import coop.local.comms.Communication;
import coop.local.comms.PiSerialCommunication;
import coop.local.mqtt.*;
import coop.local.service.PiRunner;
import coop.shared.pi.metric.Metric;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * In order to use this you need to enable the serial port on the Raspberry Pi:
 *
 * 1. On the pi execute `sudo raspi-config`
 *     *  On the raspberry pi 2 its under Interface Options -> Serial Port
 * 2. Answer NO to the question about login shell
 * 3. Answer YES to the question about serial hardware port
 * 4. Reboot
 *
 * You can get the serial port by running:
 *
 * 1. On the pi execute `dmesg | grep tty`
 * 2. The serial port will be on the line that starts with something like "3f201000.serial: ttyAMA0"
 * 3. The serial port will be after the colon, so in this case the serial port is "ttyAMA0" and the device name is
 *    "/dev/ttyAMA0"
 *
 * To test this you can connect the TX to the RX with jumper wire and then install and run minicom. What is typed should
 * be output to the screen. If it doens't work, then what you type won't show up.
 *
 *
 * minicom: https://help.ubuntu.com/community/Minicom
 * credit: https://forums.raspberrypi.com/viewtopic.php?t=213133#:~:text=Enabling%20the%20serial%20port%20is,question%20about%20serial%20hardware%20port.
 */
@Component
public class CoopRunner extends PiRunner {

    private static final Duration PUBLISH_DURATION = Duration.ofSeconds(30);
    private static final long MQTT_TIMEOUT = 5000;
    private long lastCall = 0;

    @Autowired
    private LocalStateProvider provider;

    @Autowired
    private Communication communication;

    @Override
    protected void init() {

        communication.addListener(this::onRead);
        communication.beginReading();

        // TODO: Move this somewhere else
        PiMqttMessage message = new PiMqttMessage(ShadowTopic.GET.topic(), "{}");
        publish(message);

    }

    private void onRead(byte[] str) {
        System.out.println("Received: " + new String(str));
    }

    @Override
    protected void invoke() {

        long now = System.currentTimeMillis();
        if (now - lastCall > PUBLISH_DURATION.toMillis()) {
            communication.write(32, "Hi from Pi");
            lastCall = System.currentTimeMillis();
        }






//        if (this.provider.getConfig() == null || this.provider.getConfig().getCoopId() == null) {
//            return;
//        };
    }

    @Override
    protected void handleError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @Override
    public List<ShadowSubscription> subscriptions() {
        return Arrays.asList(
                new UpdateSubscription(provider),
                new SyncConfigSubscription(provider)
        );
    }

    private void publish(String component, String metricName, double value) {
        if (this.provider.getConfig() == null || this.provider.getConfig().getCoopId() == null) {
            return;
        }

        Metric metric = new Metric();
        metric.setDt(System.currentTimeMillis());
        metric.setCoopId(this.provider.getConfig().getCoopId());
        metric.setComponentId(component);
        metric.setMetric(metricName);
        metric.setValue(value);

        PiMqttMessage message = new PiMqttMessage(ShadowTopic.METRIC.topic(), metric);
        publish(message);
    }

    private void publish(PiMqttMessage message) {
        // TODO: have to save the data locally...
        client().publish(message);
    }

    @Data
    private static class Weather {
        private Current current;
    }

    @Data
    private static class Current {
        private long time;
        private double temperature_2m;
        private double relative_humidity_2m;
    }
}
