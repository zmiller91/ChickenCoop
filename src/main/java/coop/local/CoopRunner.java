package coop.local;

import coop.device.protocol.*;
import coop.device.protocol.event.*;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageReceived;
import coop.local.database.downlink.DownlinkRepository;
import coop.local.database.job.JobRepository;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.listener.EventProcessor;
import coop.local.scheduler.DownlinkDispatcher;
import coop.local.scheduler.Scheduler;
import coop.local.service.PiRunner;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.CoopState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
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

    @Autowired
    private LocalStateProvider provider;

    @Autowired
    private Communication communication;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MetricCacheRepository metricCache;

    private long lastStateRefresh = 0;
    private final List<Invokable> invokables = new ArrayList<>();

    @Override
    protected void init() {

        communication.addListener(MessageReceived.class, this::onMessageReceived);
        communication.beginReading();
        provider.init();

        DownlinkDispatcher downlinkDispatcher = new DownlinkDispatcher(communication);
        Scheduler scheduler = new Scheduler(provider, jobRepository, downlinkDispatcher);
        MetricProcessor metricProcessor = new MetricProcessor(metricCache, provider);
        RuleProcessor ruleProcessor = new RuleProcessor(metricCache, scheduler);

        EventProcessor.addListeners(
                downlinkDispatcher,
                scheduler,
                metricProcessor,
                ruleProcessor);

        invokables.add(downlinkDispatcher);
        invokables.add(scheduler);
    }

    @Override
    protected void invoke() {
        if(provider.getConfig() == null &&
                Duration.ofSeconds(30).toMillis() < System.currentTimeMillis() - lastStateRefresh) {
            provider.refreshState();
            lastStateRefresh = System.currentTimeMillis();
        }

        invokables.forEach(Invokable::invoke);
    }

    @Override
    protected void handleError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    private void onMessageReceived(MessageReceived message) {
        System.out.println("Received: " + message.getRaw());
        CoopState coop = provider.getConfig();
        if(coop != null) {
            EventProcessor.receiveMessage(coop, message);
        }
    }
}
