package it.unibz.obfuscationapi.Events;

import it.unibz.obfuscationapi.Events.BATT.BatteryLevel;
import it.unibz.obfuscationapi.Events.BATT.BatteryStatus;
import it.unibz.obfuscationapi.Events.BOOT.BootCompleted;
import it.unibz.obfuscationapi.Events.CALL.CallEvent;
import it.unibz.obfuscationapi.Events.NET.NetEvent;
import it.unibz.obfuscationapi.Events.SMS.SmsEvent;
import it.unibz.obfuscationapi.Events.SYS.KeyEvent;

import java.util.HashMap;

public class EventCommandFactory {
    private static final HashMap<EventType, EventCommand> commandMap = new HashMap<>();

    static {
        commandMap.put(EventType.BOOT_COMPLETED, new BootCompleted());
        commandMap.put(EventType.BATT_FULL, new BatteryLevel(EventType.BATT_FULL));
        commandMap.put(EventType.BATT_HALF, new BatteryLevel(EventType.BATT_HALF));
        commandMap.put(EventType.BATT_LOW, new BatteryLevel(EventType.BATT_LOW));
        commandMap.put(EventType.BATT_EMPTY, new BatteryLevel(EventType.BATT_EMPTY));
        commandMap.put(EventType.BATT_CHARGING, new BatteryStatus());
        commandMap.put(EventType.POWER_BUTTON, new KeyEvent(EventType.POWER_BUTTON));
        commandMap.put(EventType.VOLUME_UP, new KeyEvent(EventType.VOLUME_UP));
        commandMap.put(EventType.VOLUME_DOWN, new KeyEvent(EventType.VOLUME_DOWN));
        commandMap.put(EventType.SMS, new SmsEvent());
        commandMap.put(EventType.WIFI_ENABLE, new NetEvent(NetEvent.Service.wifi, NetEvent.State.enable));
        commandMap.put(EventType.WIFI_DISABLE, new NetEvent(NetEvent.Service.wifi, NetEvent.State.disable));
        commandMap.put(EventType.DATA_ENABLE, new NetEvent(NetEvent.Service.data, NetEvent.State.enable));
        commandMap.put(EventType.DATA_DISABLE, new NetEvent(NetEvent.Service.data, NetEvent.State.disable));
        commandMap.put(EventType.BLUETOOTH_ENABLE, new NetEvent(NetEvent.Service.bluetooth, NetEvent.State.enable));
        commandMap.put(EventType.BLUETOOTH_DISABLE, new NetEvent(NetEvent.Service.bluetooth, NetEvent.State.disable));
        commandMap.put(EventType.RECEIVING_CALL, new CallEvent());
    }

    public static EventCommand getCommand(EventType type) {
        return commandMap.get(type);
    }
}
