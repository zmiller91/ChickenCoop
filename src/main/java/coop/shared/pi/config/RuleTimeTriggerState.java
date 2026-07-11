package coop.shared.pi.config;

import lombok.Data;

@Data
public class RuleTimeTriggerState
{
    private String id;
    private int hour;
    private int minute;
    private String operator;
}
