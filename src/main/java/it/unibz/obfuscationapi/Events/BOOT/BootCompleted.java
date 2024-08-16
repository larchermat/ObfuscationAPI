package it.unibz.obfuscationapi.Events.BOOT;

import it.unibz.obfuscationapi.Events.EventCommand;

public class BootCompleted implements EventCommand {
    @Override
    public String getCommand() {
        return "am broadcast -a android.intent.action.BOOT_COMPLETED";
    }
}
