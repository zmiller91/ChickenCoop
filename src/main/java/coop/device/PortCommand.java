package coop.device;

/**
 * What a DownlinkFrame represents in port-oriented terms - which action and which port it targets.
 * Used to report a command's outcome (completed/failed/cancelled) back up without the reporter needing to
 * know the device-specific frame format.
 */
public record PortCommand(String actionKey, int portIndex) {
}
