package coop.local;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import coop.device.Actuator;
import coop.device.protocol.*;
import coop.local.cache.MetricCache;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageReceived;
import coop.local.service.PiRunner;
//import coop.shared.database.repository.*;
//import coop.shared.database.table.*;
import coop.local.state.LocalStateProvider;
import coop.device.DeviceType;
import coop.shared.database.table.rule.Operator;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.RuleState;
import coop.shared.pi.metric.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private MetricCache metricCache;

    @Autowired
    private CommandQueue commandQueue;

    private long lastStateRefresh = 0;



    @Override
    protected void init() {
        communication.addListener(MessageReceived.class, this::onMessageReceived);
        communication.beginReading();
        provider.init();
    }

    @Override
    protected void invoke() {
        if(provider.getConfig() == null &&
                Duration.ofSeconds(30).toMillis() < System.currentTimeMillis() - lastStateRefresh) {
            provider.refreshState();
            lastStateRefresh = System.currentTimeMillis();
        }

        commandQueue.sendNext();
    }

    @Override
    protected void handleError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    private void onMessageReceived(MessageReceived message) {
        System.out.println("Received: " + message.getRaw());

        CoopState coop = provider.getConfig();
        if(coop != null) {

            UplinkFrame frame = new UplinkFrame(message.getMessage());
            if(frame.getSerialNumber() != null) {

                ComponentState component = coop
                        .getComponents()
                        .stream()
                        .filter(c -> c.getSerialNumber().equals(frame.getSerialNumber()))
                        .findFirst()
                        .orElse(null);

                if(component != null) {

                    DeviceType deviceType = component.getDeviceType();
                    EventParser parser = deviceType.getDevice().getEventParser();
                    if(parser != null) {

                        List<Event> events = parser.parse(frame);
                        if (events != null) {

                            for (Event event : events) {

                                if(event instanceof MetricEvent metric) {
                                    processMetricEvent(coop, component, metric);
                                }

                                if(event instanceof CommandRequestEvent cmdRequest) {
                                    processCommandRequest(coop, component, cmdRequest);
                                }

                                if(event instanceof AckEvent ack) {
                                    commandQueue.ack(ack.getMessageId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void processCommandRequest(CoopState coop, ComponentState component, CommandRequestEvent event) {


        //TODO: I think we are going to run into an issue where a device has several sets of commands and this implementation
        //      is going to cause commands to be dropped. For example, consider this sequence of commands:
        //          - OPEN_VALVE
        //          - END
        //          - OPEN_VALVE
        //          - END
        //      When the command queue runs it will send all of those, but the device will turn off its radio at the
        //      first 'END' causing the second OPEN_VALVE to drop/not be acked. This may not be an issue for rules that
        //      are based on sensor data, since those will just re-trigger next time, but it could cause issues with
        //      scheduled rules.
        //      .
        //      We May need some callback to mark that the action was executed and if not, we re-send it.

        // Only actuators can have command sent to them
        if(!(component.getDeviceType().getDevice() instanceof Actuator device)) {
            return;
        }

        // Find all rules that have been satisfied
        // Find all actions from those satisfied rules
        // Filter to only actions related to the component requesting a command
        // Create the commands
        coop.getRules()
                .stream()
                .filter(this::isRuleSatisfied)
                .flatMap(rule -> rule.getActions().stream())
                .filter(action -> action.getComponentId().equals(component.getComponentId()))
                .forEach(action -> {
                    JsonObject actionBody = parseActionBody(action.getAction());
                    if(actionBody != null) {
                        DownlinkFrame downlink = device.createCommand(component.getSerialNumber(), actionBody);
                        commandQueue.add(downlink);
                    }
                });

        // Always send an END to acknowledge completion of the request
        commandQueue.add(new EndDownlink(component.getSerialNumber()));
    }

    private JsonObject parseActionBody(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRuleSatisfied(RuleState rule) {
        return rule.getComponentTriggers().stream().allMatch(trigger -> {
            Double metricValue = metricCache.get(trigger.getComponentId(), trigger.getMetric());
            return metricValue != null && Operator.valueOf(trigger.getOperator()).evaluate(metricValue, trigger.getThreshold());
        });
    }

    private void processMetricEvent(CoopState coop, ComponentState component, MetricEvent event) {

        Metric metric = new Metric();
        metric.setDt(System.currentTimeMillis());
        metric.setCoopId(coop.getCoopId());
        metric.setComponentId(component.getComponentId());
        metric.setMetric(event.getMetric());
        metric.setValue(event.getValue());

        provider.save(metric);
        metricCache.put(component.getComponentId(), event.getMetric(), event.getValue());
    }
}
