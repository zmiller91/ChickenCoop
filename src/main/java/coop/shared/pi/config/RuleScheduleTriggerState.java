package coop.shared.pi.config;

import lombok.Data;

@Data
public class RuleScheduleTriggerState
{
    private String id;
    private String frequency;
    private int hour;
    private int minute;
    private int gap;
}
