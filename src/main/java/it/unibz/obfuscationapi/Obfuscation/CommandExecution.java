package it.unibz.obfuscationapi.Obfuscation;

import java.io.*;
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

    /**
     * Cleans up the decompiled directory deleting the packages added because of the transformations and reverting
     * changes using git
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void cleanUp() throws IOException, InterruptedException {
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "cleanUp.cmd"};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join(" ", cmd) + "\" failed");
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            File file = new File(Paths.get("scripts", "unix").toString());
            String[] cmd = {"bash", "cleanUp.sh"};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Unix command \"" + String.join(" ", cmd) + "\" failed");
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
    }

    /**
     * Runs the script to wipe the device's data, start it once without loading any snapshot, install strace on it and
     * exit saving the snapshot that will be reused for every execution
     *
     * @param avd the name of the emulated device (usually model_API, Pixel_6_API_33)
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Starts the device loading the initial snapshot, installs the APK and grants (if any) the needed permissions to
     * run the application
     *
     * @param appName         name of the APK
     * @param avd             name of the emulated device
     * @param pkgName         name of the application package
     * @param permissionsList list of permissions that the application needs (empty if there are no needed permissions)
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Needs to be executed when the device is turned on (after executing installAPK)
     * Runs the script that starts the application attaching strace to it, records the execution after sending an event
     * (if any), saves the log in the specified file and closes the emulator without saving
     *
     * @param pkgName      name of the application package
     * @param mainActivity entry point of the application
     * @param pathToLog    path where we want to store the log locally
     * @param aEScript     script that is executed by adb to trigger an event
     * @throws IOException
     * @throws InterruptedException
     */
    public static void generateLog(String pkgName, String mainActivity, String pathToLog, String aEScript) throws IOException, InterruptedException {
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "generateLog.cmd", pkgName, mainActivity, pathToLog, aEScript};
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

    /**
     * Executes a command and returns the exit code
     *
     * @param args array containing the instructions that compose the command
     * @param file working directory in which the command is executed
     * @return the exit code
     * @throws IOException
     * @throws InterruptedException
     */
    private static int execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);
        return pr.waitFor();
    }
}
