package it.unibz.obfuscationapi.Obfuscation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

public class Obfuscation {
    private final String path;
    private final ArrayList<File> smaliFiles;
    private final String dexDump;

    public Obfuscation(String path) {
        this.path = path;
        smaliFiles = searchFiles(new File(path), ".smali", null, null);
        try {
            dexDump = generateDexDump();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Obfuscation obfuscation = new Obfuscation("/Users/matteolarcher/Desktop/UNIBZ/tesi/work_dir/apks/app-release");
        obfuscation.getMethodNumber();
    }

    private String generateDexDump() throws InterruptedException, IOException {
        File workingDir = new File(path + "/dist");
        Path dump = Paths.get(path, "/dist/dump.txt");
        if (Files.exists(dump))
            return path + "/dist/dump.txt";
        String[] cmd1 = {"cp", "app-release.apk", "app-release.zip"};
        if (execCommand(cmd1, workingDir) != 0)
            return null;
        System.out.println("cp succeed");
        String[] cmd2 = {"unzip", "app-release.zip", "-d", "decomp/"};
        if (execCommand(cmd2, workingDir) != 0)
            return null;
        System.out.println("unzip succeed");
        String[] cmd3 = {"/bin/sh", "-c", "~/Library/Android/sdk/build-tools/35.0.0/dexdump decomp/classes.dex > dump.txt"};
        if (execCommand(cmd3, workingDir) != 0)
            return null;
        System.out.println("dexdump succeed");
        String[] cmd4 = {"rm", "app-release.zip"};
        if (execCommand(cmd4, workingDir) != 0)
            return null;
        System.out.println("first rm succeed");
        String[] cmd5 = {"rm", "-r", "decomp"};
        if (execCommand(cmd5, workingDir) != 0)
            return null;
        System.out.println("second rm succeed");

        return path + "/dist/dump.txt";
    }

    private int execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);
        return pr.waitFor();
    }

    public int getMethodNumber() throws FileNotFoundException, UnsupportedEncodingException {
        StringBuffer dump = getStringBufferFromFile(dexDump);
        Pattern pattern = Pattern.compile("(Class #[0-9]+)(?s)(.*?)(source_file_idx.*?\\))");
        Matcher matcher = pattern.matcher(dump);
        while (matcher.find()) {

        }
        return 0;
    }

}
