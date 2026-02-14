package coop.local.service;


import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Component
@Transactional
public abstract class PiRunner implements Runnable {

    private boolean shouldRun = true;
    private boolean isRunning = true;

    public void run() {
        this.shouldRun = true;
        this.isRunning = true;

        init();

        while(this.shouldRun) {
            try {
                sleep(1000);
                invoke();
            } catch (Throwable t) {
                log.warn(t);
                t.printStackTrace();
                handleError(t);
            }
        }

        this.isRunning = false;
    }

    public void stop() {
        this.shouldRun = true;
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void init();
    protected abstract void invoke();
    protected abstract void handleError(Throwable t);


}
