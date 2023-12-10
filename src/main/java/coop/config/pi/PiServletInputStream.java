package coop.config.pi;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PiServletInputStream extends ServletInputStream {

    private int lastIndexRetrieved = -1;
    private ReadListener readListener;
    private final byte[] data;

    public PiServletInputStream(byte[] data) {
            this.data = data;
    }

    @Override
    public boolean isFinished() {
        return (lastIndexRetrieved == data.length - 1);
    }

    @Override
    public boolean isReady() {
        return isFinished();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
        updateReadListener();
    }

    @Override
    public int read() {
        if (!isFinished()) {
            int i = data[lastIndexRetrieved + 1];
            lastIndexRetrieved++;
            updateReadListener();
            return i;
        }

        return -1;
    }

    private void updateReadListener() {
        try {
            if (readListener != null) {
                if (isFinished()) {
                    readListener.onAllDataRead();
                }
                else if (lastIndexRetrieved >= 0) {
                    readListener.onDataAvailable();
                }
            }
        } catch (IOException e) {
            readListener.onError(e);
        }
    }
}
