package it.unibz.obfuscationapi.Events;

/**
 * Enumeration composed of types of event we can send to the device while it executes the APK, to trigger the reaction
 * from the application
 */
public enum EventType {
    BOOT_COMPLETED,
    BATT_FULL,
    BATT_HALF,
    BATT_LOW,
    BATT_CHARGING,
    POWER_BUTTON,
    VOLUME_UP,
    VOLUME_DOWN,
    SMS,
    WIFI_ENABLE,
    WIFI_DISABLE,
    DATA_ENABLE,
    DATA_DISABLE,
    BLUETOOTH_ENABLE,
    BLUETOOTH_DISABLE,
    RECEIVING_CALL
}
