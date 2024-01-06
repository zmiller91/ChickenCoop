package coop.local.service;

import coop.local.mqtt.PiMqttClient;
import coop.local.mqtt.ShadowSubscription;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Component
@Transactional
public abstract class PiRunner implements Runnable {

    @Autowired
    private PiMqttClient client;

    private boolean shouldRun = true;
    private boolean isRunning = true;

    public void run() {
        this.shouldRun = true;
        this.isRunning = true;

        connect();
        init();

        while(this.shouldRun) {
            try {
                sleep(1000);
                invoke();
            } catch (Throwable t) {
                log.warn(t);
                handleError(t);
            }
        }

        this.isRunning = false;
    }

    public void stop() {
        this.shouldRun = true;
    }

    private void connect() {
        try {

            client().withSubscriptions(subscriptions()).connect();

            // TODO: Apparently I need to wait a second before the subscription to take effect...
            sleep(2000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected PiMqttClient client() {
        if (this.client == null) {
            throw new IllegalStateException("Client is not initialized.");
        }

        return this.client;
    }

    protected abstract void init();
    protected abstract void invoke();
    protected abstract void handleError(Throwable t);
    protected abstract List<ShadowSubscription> subscriptions();


}
