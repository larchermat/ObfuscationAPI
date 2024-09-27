package it.unibz.obfuscationapi.Events;

public interface EventCommand {
    /**
     * Method that returns a string with the command to run to send the event to the AVD
     *
     * @return the string containing the command
     */
    String getCommand();
}
