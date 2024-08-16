package it.unibz.obfuscationapi.Events.BATT;

import it.unibz.obfuscationapi.Events.EventCommand;

public class BatteryStatus implements EventCommand {
    @Override
    public String getCommand() {
        return "dumpsys battery set status 2";
    }
}
