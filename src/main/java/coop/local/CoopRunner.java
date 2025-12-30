package coop.local;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import coop.device.Actuator;
import coop.device.protocol.*;
import coop.device.protocol.command.EndCommandDownlink;
import coop.device.protocol.event.*;
import coop.device.protocol.parser.EventParser;
import coop.local.cache.MetricCache;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageReceived;
import coop.local.scheduler.CommandQueue;
import coop.local.scheduler.Scheduler;
import coop.local.service.PiRunner;
//import coop.shared.database.repository.*;
//import coop.shared.database.table.*;
import coop.local.state.LocalStateProvider;
import coop.device.types.DeviceType;
import coop.shared.database.table.Coop;
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
    private Scheduler scheduler;

    private long lastStateRefresh = 0;


    @Override
    protected void init() {
        communication.addListener(MessageReceived.class, this::onMessageReceived);
        communication.beginReading();
        provider.init();

        EventListener.addListener(MetricEvent.class, this::processMetricEvent);
        EventListener.addListener(CommandRequestEvent.class, this::processCommandRequest);
    }

    @Override
    protected void invoke() {
        if(provider.getConfig() == null &&
                Duration.ofSeconds(30).toMillis() < System.currentTimeMillis() - lastStateRefresh) {
            provider.refreshState();
            lastStateRefresh = System.currentTimeMillis();
        }

        scheduler.invoke();
    }

    @Override
    protected void handleError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    private void onMessageReceived(MessageReceived message) {
        System.out.println("Received: " + message.getRaw());
        CoopState coop = provider.getConfig();
        if(coop != null) {
            EventListener.receiveMessage(coop, message);
        }
    }

    private void processCommandRequest(EventPayload message) {

        if(!(message.getEvent() instanceof CommandRequestEvent event)) {
            return;
        }

        CoopState coop = message.getCoop();
        ComponentState component = message.getComponent();

        // Only actuators can have command sent to them
        if(!(component.getDeviceType().getDevice() instanceof Actuator device)) {
            return;
        }


        // TODO: Check if any commands have been reserved/allocated

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
                        scheduler.create(component, downlink, action.getId());
                    }
                });

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

    private void processMetricEvent(EventPayload message) {
        if(!(message.getEvent() instanceof MetricEvent event)) {
            return;
        }

        Metric metric = new Metric();
        metric.setDt(System.currentTimeMillis());
        metric.setCoopId(message.getCoop().getCoopId());
        metric.setComponentId(message.getComponent().getComponentId());
        metric.setMetric(event.getMetric());
        metric.setValue(event.getValue());

        provider.save(metric);
        metricCache.put(message.getComponent().getComponentId(), event.getMetric(), event.getValue());
    }
}
