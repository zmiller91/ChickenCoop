package coop.device;

import com.google.gson.JsonObject;
import coop.device.protocol.DownlinkFrame;

import java.util.List;
import java.util.Map;

public interface Actuator {
    boolean validateCommand(String commandName, Map<String, String> params);
    DownlinkFrame createCommand(String serialNumber, String commandName, Map<String, String> params);
    List<Action> getActions();
}
