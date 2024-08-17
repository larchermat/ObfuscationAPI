package it.unibz.obfuscationapi.Obfuscation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CommandExecution {
    public static final String os = System.getProperty("os.name").toLowerCase();

    /**
     * Executes the scripts in the scripts folder to decompile the APK and generate the dumps of the dex files
     *
     * @param pathToApk path to the original APK to decompile
     * @throws IOException
     * @throws InterruptedException
     */
    public static void decompileAPK(String pathToApk) throws IOException, InterruptedException {
        // TODO: add the repo initialization to the bash and cmd scripts
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "decompileAPK.cmd", pathToApk};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "decompileAPK.sh", pathToApk};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Mac command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    /**
     * Executes the scripts in the scripts folder to rebuild and sign the APK
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void rebuildAPK(String appName) throws IOException, InterruptedException {
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "rebuildAPK.cmd", appName};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            File file = new File(Paths.get("scripts", "unix").toString());
            String[] cmd = {"bash", "rebuildAPK.sh", appName};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Unix command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    public static void prepareDevice(String avd) throws IOException, InterruptedException {
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "prepareDevice.cmd", avd};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "prepareDevice.sh", avd};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Mac command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    public static void installAPK(String appName, String avd, String pkgName, ArrayList<String> permissionsList) throws IOException, InterruptedException {
        String permissions = permissionsList.isEmpty() ? "" : ("\"" + String.join(" ", permissionsList) + "\"");
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "installAPK.cmd", appName, avd, pkgName, permissions};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "installAPK.sh", appName, avd, pkgName, permissions};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Mac command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    public static void generateLog(String pkgName, String mainActivity, String pathToLog, String aEScript) throws IOException, InterruptedException {
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "generateLog.cmd", pkgName, mainActivity, aEScript, pathToLog};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "generateLog.sh", pkgName, mainActivity, pathToLog, aEScript};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Mac command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    private static int execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);
        return pr.waitFor();
    }
}
