package coop.device;

import coop.device.moisture.MoistureSensor;
import coop.device.scale.ScaleSensor;
import coop.device.valve.ValveActuator;
import coop.device.weather.WeatherSensor;

import java.util.stream.Stream;

public enum DeviceType {

    WEATHER(new WeatherSensor()),
    SCALE(new ScaleSensor()),
    MOISTURE(new MoistureSensor()),
    VALVE(new ValveActuator());

    private final Device device;

    DeviceType(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    public static DeviceType getByName(String name) {
        return Stream.of(DeviceType.values())
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(null);
    }

}
