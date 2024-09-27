package it.unibz.obfuscationapi.Events.BATT;

import it.unibz.obfuscationapi.Events.EventCommand;
import it.unibz.obfuscationapi.Events.EventType;

public class BatteryLevel implements EventCommand {
    private final int lvl;

    public BatteryLevel(EventType battLvl) {
        this.lvl = switch (battLvl) {
            case BATT_FULL -> 100;
            case BATT_HALF -> 50;
            case BATT_LOW -> 19;
            default -> throw new IllegalStateException("Unexpected value: " + battLvl);
        };
    }

    @Override
    public String getCommand() {
        return "dumpsys battery set level " + lvl;
    }
}
