package it.unibz.obfuscationapi.Events.CALL;

import it.unibz.obfuscationapi.Events.EventCommand;

public class CallEvent implements EventCommand {
    @Override
    public String getCommand() {
        return "am broadcast -a android.intent.action.PHONE_STATE -e state RINGING";
    }
}
