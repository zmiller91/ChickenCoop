package coop.device;

import com.google.gson.JsonObject;
import coop.device.protocol.DownlinkFrame;

public interface Actuator {
    boolean validateCommand(JsonObject object);
    DownlinkFrame createCommand(String serialNumber, JsonObject object);
}
