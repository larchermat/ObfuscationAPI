import it.unibz.obfuscationapi.Events.EventType;
import it.unibz.obfuscationapi.Obfuscation.CommandExecution;
import it.unibz.obfuscationapi.Obfuscation.Obfuscation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Experiment {
    static ArrayList<EventType> BOOT = new ArrayList<>(List.of(EventType.BOOT_COMPLETED));
    static ArrayList<EventType> SMS = new ArrayList<>(List.of(EventType.SMS));
    static ArrayList<EventType> NET = new ArrayList<>(List.of(EventType.WIFI_DISABLE, EventType.WIFI_ENABLE, EventType.DATA_DISABLE, EventType.DATA_ENABLE));
    static ArrayList<EventType> CALL = new ArrayList<>(List.of(EventType.RECEIVING_CALL));
    static ArrayList<EventType> SYS = new ArrayList<>(List.of(EventType.POWER_BUTTON, EventType.VOLUME_DOWN, EventType.VOLUME_UP));
    static ArrayList<EventType> BATT = new ArrayList<>(List.of(EventType.BATT_CHARGING, EventType.BATT_FULL, EventType.BATT_HALF, EventType.BATT_LOW));
    String pathToApk;
    ArrayList<EventType> eventTypes;
    String family;

    public Experiment(String pathToApk, ArrayList<String> eventTypes, String family) {
        File apkDir = new File(pathToApk);
        this.pathToApk = Objects.requireNonNull(apkDir.listFiles())[0].getAbsolutePath();
        this.eventTypes = new ArrayList<>();
        for (String eventType : eventTypes) {
            if (eventType.equals("BOOT"))
                this.eventTypes.addAll(BOOT);
            else if (eventType.equals("SMS"))
                this.eventTypes.addAll(SMS);
            else if (eventType.equals("NET"))
                this.eventTypes.addAll(NET);
            else if (eventType.equals("CALL"))
                this.eventTypes.addAll(CALL);
            else if (eventType.equals("SYS"))
                this.eventTypes.addAll(SYS);
            else if (eventType.equals("BATT"))
                this.eventTypes.addAll(BATT);
        }
        this.family = family;
    }

    public static void main(String[] args) {
        int avdNum = 25;
        String avdName = "Device_API_24";
        String sysImage = "system-images;android-24;default;x86_64";
        ArrayList<Experiment> experiments = new ArrayList<>(List.of(
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Airpush", new ArrayList<>(List.of("BOOT", "BATT", "SMS")), "Airpush"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Dowgin", new ArrayList<>(List.of("SYS")), "Dowgin"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/FakeDoc", new ArrayList<>(List.of("BOOT", "SMS", "BATT")), "FakeDoc"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/FakeInst", new ArrayList<>(List.of("BOOT", "CALL")), "FakeInst"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/FakePlayer", new ArrayList<>(List.of("BOOT", "SYS", "BATT")), "FakePlayer"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/FakeTimer", new ArrayList<>(List.of("BOOT", "SYS", "SMS")), "FakeTimer"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/FakeUpdates", new ArrayList<>(List.of("BOOT", "CALL")), "FakeUpdates"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Fobus", new ArrayList<>(List.of("BOOT", "SMS", "NET", "BATT")), "Fobus"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/GingerMaster", new ArrayList<>(List.of("BOOT")), "GingerMaster"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/GoldDream", new ArrayList<>(List.of("BOOT", "BATT", "SMS")), "GoldDream"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Kuguo", new ArrayList<>(List.of("BOOT", "SMS", "NET", "BATT")), "Kuguo"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Kyview", new ArrayList<>(List.of("BOOT", "SMS", "NET", "BATT")), "Kyview"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Lnk", new ArrayList<>(List.of("BOOT")), "Lnk"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Lotoor", new ArrayList<>(List.of("BOOT", "SMS", "BATT")), "Lotoor"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Minimob", new ArrayList<>(List.of("BOOT")), "Minimob"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Youmi", new ArrayList<>(List.of("BOOT", "SMS", "CALL")), "Youmi"),
                new Experiment("/home/mlarcher/Desktop/tesi/APKs/Zitmo", new ArrayList<>(List.of("BOOT", "SYS", "BATT")), "Zitmo")
        ));
        for (Experiment experiment : experiments) {
            try {
                Obfuscation obfuscation = new Obfuscation(experiment.pathToApk, avdNum, avdName, 30, experiment.eventTypes, true, experiment.family);
                obfuscation.addAllTransformations();
                obfuscation.startSampling(true);
                CommandExecution.shutdownEmus();
                //obfuscation.startSampling(false);
                //obfuscation.setTransform(true);
                //obfuscation.setLogsPerCase(10);
                //obfuscation.addAllTransformations();
                //obfuscation.startSampling(false);
                //CommandExecution.shutdownEmus();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}