package coop.local.comms;

import com.fazecast.jSerialComm.SerialPort;

public class DevSerialCommunication implements SerialCommunication {

    private final SerialPort serialPort;

    public DevSerialCommunication(String device) {
        this(device, 115200);
    }

    public DevSerialCommunication(String device, int baud) {
        serialPort = SerialPort.getCommPort(device);
        serialPort.setBaudRate(baud);
        serialPort.setNumStopBits(1);
        serialPort.setParity(0);
        serialPort.setFlowControl(0);
        serialPort.setNumDataBits(8);

        serialPort.openPort();
    }

    @Override
    public boolean bytesAvailable() {
        return serialPort.bytesAvailable() > 0;
    }

    @Override
    public byte[] readBytes() {

        int available = serialPort.bytesAvailable();
        byte[] buffer = new byte[available];
        int read = serialPort.readBytes(buffer, available);

        // todo: if read < available

        return buffer;
    }

    @Override
    public int write(byte[] bytes) {
        return serialPort.writeBytes(bytes, bytes.length);
    }
}
