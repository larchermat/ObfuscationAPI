package it.unibz.obfuscationapi.Events.NET;

import it.unibz.obfuscationapi.Events.EventCommand;

public class NetEvent implements EventCommand {
    private final Service service;
    private final State state;

    public enum Service {
        wifi,
        data,
        bluetooth
    }

    public enum State {
        enable,
        disable
    }

    public NetEvent(Service service, State state) {
        this.service = service;
        this.state = state;
    }

    @Override
    public String getCommand() {
        return "svc " + service + " " + state;
    }
}
