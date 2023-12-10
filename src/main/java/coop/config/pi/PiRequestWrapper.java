package coop.config.pi;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PiRequestWrapper extends HttpServletRequestWrapper {

    private ServletInputStream is;
    private byte[] data;

    public PiRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    public ServletInputStream getInputStream() throws IOException {

        if (is == null) {
            is = new PiServletInputStream(this.data);
        }

        return is;
    }


    public void setData(byte[] data) {
        this.data = data;
    }
}
