package it.unibz.obfuscationapi.Events.SYS;

import it.unibz.obfuscationapi.Events.EventCommand;
import it.unibz.obfuscationapi.Events.EventType;

public class KeyEvent implements EventCommand {
    private final int keyEvent;

    public KeyEvent(EventType keyEvent) {
        this.keyEvent = switch (keyEvent) {
            case POWER_BUTTON -> 26;
            case VOLUME_UP -> 24;
            case VOLUME_DOWN -> 25;
            default -> throw new IllegalStateException("Unexpected value for KeyEvent: " + keyEvent);
        };
    }

    @Override
    public String getCommand() {
        return "input keyevent " + keyEvent;
    }
}
