package coop.pi.config;

import lombok.Data;

@Data
public class IotState {
    private CoopConfig desired;
    private CoopConfig reported;
}
