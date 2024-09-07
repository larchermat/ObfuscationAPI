import it.unibz.obfuscationapi.Obfuscation.Obfuscation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        Experiment experiment = new Experiment("/Users/matteolarcher/Desktop/UNIBZ/tesi/project/ObfuscationAPI/APKs/Airpush", new ArrayList<>(List.of("BOOT", "SMS", "BATT", "SYS")), "Airpush");
        int avdNum = 3;
        String avdName = "Pixel_6_API_33";
        String sysImage = "system-images;android-33;default;arm64-v8a";
        try {
            Obfuscation obfuscation = new Obfuscation(experiment.pathToApk, avdNum, avdName, 1, experiment.eventTypes, false, experiment.family);
            obfuscation.createDevices(sysImage);
            obfuscation.addAllTransformations();
            obfuscation.startSampling(true);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
