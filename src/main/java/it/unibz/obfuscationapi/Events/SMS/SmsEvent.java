package it.unibz.obfuscationapi.Events.SMS;

import it.unibz.obfuscationapi.Events.EventCommand;

public class SmsEvent implements EventCommand {
    @Override
    public String getCommand() {
        return "am broadcast -a android.provider.Telephony.SMS_RECEIVED";
    }
}
