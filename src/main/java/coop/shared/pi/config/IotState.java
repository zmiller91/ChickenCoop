package coop.shared.pi.config;

import lombok.Data;

@Data
public class IotState {
    private CoopState desired;
    private CoopState reported;
}
