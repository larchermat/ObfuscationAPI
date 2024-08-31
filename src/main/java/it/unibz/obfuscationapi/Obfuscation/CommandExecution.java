package it.unibz.obfuscationapi.Obfuscation;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.LS;
import static it.unibz.obfuscationapi.Utility.Utilities.writeErrorLog;
import static java.lang.Thread.currentThread;

public class CommandExecution {
    public static final String os = System.getProperty("os.name").toLowerCase();

    /**
     * Executes the scripts in the scripts folder to decompile the APK and generate the dumps of the dex files
     *
     * @param pathToApk path to the original APK to decompile
     * @throws IOException
     * @throws InterruptedException
     */
    public static void decompileAPK(String pathToApk, String appName) throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "decompileAPK.cmd", pathToApk, appName};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "decompileAPK.sh", pathToApk, appName};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "decompileAPK");
    }

    /**
     * Executes the scripts in the scripts folder to rebuild and sign the APK
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void rebuildAPK(String appName, String obfuscation) throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "rebuildAPK.cmd", appName, obfuscation};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            File file = new File(Paths.get("scripts", "unix").toString());
            String[] cmd = {"bash", "rebuildAPK.sh", appName, obfuscation};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "rebuildAPK");
    }

    /**
     * Cleans up the decompiled directory deleting the packages added because of the transformations and reverting
     * changes using git
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void cleanUp() throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "cleanUp.cmd"};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            File file = new File(Paths.get("scripts", "unix").toString());
            String[] cmd = {"bash", "cleanUp.sh"};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "cleanUp");
    }

    /**
     * Runs the script to wipe the device's data, start it once without loading any snapshot, and exit, saving the
     * snapshot that will be reused for every execution
     *
     * @param avd the name of the emulated device (usually model_API, Pixel_6_API_33)
     * @throws IOException
     * @throws InterruptedException
     */
    public static void prepareDevice(String avd, int port) throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "prepareDevice.cmd", avd};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "prepareDevice.sh", avd, String.valueOf(port)};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "prepareDevice");
    }

    /**
     * Starts the device loading the initial snapshot, installs the APK and grants (if any) the needed permissions to
     * run the application
     *
     * @param appName         name of the APK
     * @param avd             name of the emulated device
     * @param port            port of the emulated device
     * @throws IOException
     * @throws InterruptedException
     */
    public static void installAPK(String appName, String avd, int port) throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "installAPK.cmd", appName, avd};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "installAPK.sh", appName, avd, String.valueOf(port)};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "installAPK");
    }

    /**
     * Needs to be executed when the device is turned on (after executing installAPK)
     * Runs the script that starts the application attaching strace to it, records the execution after sending an event
     * (if any), saves the log in the specified file and closes the emulator without saving
     *
     * @param pkgName      name of the application package
     * @param mainActivity entry point of the application
     * @param pathToLog    path where we want to store the log locally
     * @param port         port of the emulated device
     * @param aEScript     script that is executed by adb to trigger an event
     * @throws IOException
     * @throws InterruptedException
     */
    public static void generateLog(String pkgName, String mainActivity, String pathToLog, int port, String aEScript) throws IOException, InterruptedException {
        String errorLog;
        int retCode;
        String command;
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "generateLog.cmd", pkgName, mainActivity, pathToLog, aEScript};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Path path = Paths.get("scripts", "unix");
            if (os.contains("mac"))
                path = path.resolve("mac");
            else
                path = path.resolve("linux");
            File file = new File(path.toString());
            String[] cmd = {"bash", "generateLog.sh", pkgName, mainActivity, pathToLog, String.valueOf(port), aEScript};
            String[] ret = execCommand(cmd, file);
            retCode = Integer.parseInt(ret[0]);
            errorLog = ret[1];
            command = String.join(" ", cmd);
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        reportError(errorLog, retCode, command, "generateLog");
    }

    private static void reportError(String errorLog, int retCode, String command, String methodName) throws IOException {
        if (!errorLog.isBlank()) {
            StringBuilder args = new StringBuilder();
            args.append("Thread: ").append(currentThread()).append(LS)
                    .append("Time: ").append(new Date(System.currentTimeMillis())).append(LS)
                    .append("Method: ").append(methodName).append(LS)
                    .append("Command: ").append(command).append(LS)
                    .append("Error: ").append(LS).append(errorLog).append(LS);
            writeErrorLog(args);
        }
        if (retCode != 0) {
            throw new RuntimeException(os + " command \"" + command + "\" failed");
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
    private static String[] execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(pr.getErrorStream()));

        String s;
        StringBuilder sb = new StringBuilder();
        String log = "";
        while ((s = stdError.readLine()) != null) {
            sb.append(s).append(LS);
        }
        if (!sb.isEmpty()) {
            String reg = "(adb: device offline)|" +
                    "(adb: device .*? not found)|" +
                    "(.*?strace_output\\.txt: 1 file pulled, 0 skipped.*?\\))|" +
                    "(.*?strace: 1 file pushed, 0 skipped.*?\\))|" +
                    "(All files should be loaded\\. Notifying the device\\.)|" +
                    "(strace: Process [0-9]+ attached)|" +
                    "(^$)";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(sb);
            log = matcher.replaceAll("").trim();
        }
        return new String[]{String.valueOf(pr.waitFor()), log};
    }
}
