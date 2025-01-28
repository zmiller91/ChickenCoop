package coop.local;

import coop.local.comms.Communication;
import coop.local.comms.message.MessageReceived;
import coop.local.comms.message.parsers.MessageParser;
import coop.local.comms.message.parsers.ParsedMessage;
import coop.local.mqtt.*;
import coop.local.service.PiRunner;
import coop.shared.database.repository.*;
import coop.shared.database.table.*;
import coop.shared.pi.metric.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@EnableTransactionManagement
public class CoopRunner extends PiRunner {

    @Autowired
    private LocalStateProvider provider;

    @Autowired
    private Communication communication;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ComponentSerialRepository componentSerialRepository;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private PiContext piContext;

    @Override
    protected void init() {

        communication.addListener(MessageReceived.class, this::onMessageReceived);
        communication.beginReading();

        // TODO: Move this somewhere else
        PiMqttMessage message = new PiMqttMessage(ShadowTopic.GET.topic(), "{}");
        publishToMqtt(message);

    }

    @Override
    protected void invoke() {

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

    private void onMessageReceived(MessageReceived message) {
        System.out.println("Received: " + message.getRaw());

        Coop coop = coop();
        if(coop != null) {

            String componentSerial = MessageParser.getComponentSerial(message);
            if(componentSerial != null) {

                ComponentSerial serial = componentSerialRepository.findById(componentSerial);
                if(serial != null) {

                    ComponentType componentType = serial.getComponentType();
                    MessageParser parser = MessageParser.forComponentType(componentType);
                    if(parser != null) {

                        List<ParsedMessage> parsedMessages = parser.parse(message);
                        CoopComponent component = componentRepository.findBySerialNumber(coop, serial);
                        if (component != null && parsedMessages != null) {

                            for (ParsedMessage parsed : parsedMessages) {
                                Metric metric = new Metric();
                                metric.setDt(System.currentTimeMillis());
                                metric.setCoopId(coop.getId());
                                metric.setComponentId(component.getComponentId());
                                metric.setMetric(parsed.getMetric());
                                metric.setValue(parsed.getValueAsDouble());

                                publishMetricToMqtt(metric);
                                saveMetric(metric);
                            }
                        }
                    }
                }
            }
        }
    }

    private void publishMetricToMqtt(Metric metric) {
        PiMqttMessage mqttMessage = new PiMqttMessage(ShadowTopic.METRIC.topic(), metric);
        client().publish(mqttMessage);
    }

    private void publishToMqtt(PiMqttMessage message) {
        client().publish(message);
    }

    private void saveMetric(Metric metric) {
        metricRepository.save(
                coop(),
                metric.getComponentId(),
                System.currentTimeMillis(),
                metric.getMetric(),
                metric.getValue());
    }

    private Coop coop() {
        if (this.provider.getConfig() == null || this.provider.getConfig().getCoopId() == null) {
            return null;
        }

        Pi pi = piRepository.findById(piContext.piId());
        return coopRepository.findById(pi, this.provider.getConfig().getCoopId());
    }
}
