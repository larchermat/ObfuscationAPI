import it.unibz.obfuscationapi.Obfuscation.Obfuscation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        Experiment experiment = new Experiment("/home/mlarcher/Desktop/tesi/APKs/Dowgin", new ArrayList<>(List.of("BOOT", "SMS", "SYS")), "Dowgin");
        int avdNum = 5;
        String avdName = "Device";
        try {
            Obfuscation obfuscation = new Obfuscation(experiment.pathToApk, avdNum, avdName, 1, experiment.eventTypes, true, experiment.family);
            obfuscation.addAllTransformations();
            obfuscation.startSampling(false);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
