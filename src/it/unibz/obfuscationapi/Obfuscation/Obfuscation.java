package it.unibz.obfuscationapi.Obfuscation;

import it.unibz.obfuscationapi.CallIndirection.CallIndirection;
import it.unibz.obfuscationapi.CodeReorder.CodeReorder;
import it.unibz.obfuscationapi.IdentifierRenaming.IdentifierRenaming;
import it.unibz.obfuscationapi.JunkInsertion.Insertion.Insertion;
import it.unibz.obfuscationapi.JunkInsertion.NopToJunk.NopToJunk;
import it.unibz.obfuscationapi.StringEncryption.StringEncryption;
import it.unibz.obfuscationapi.Transformation.Transformation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

public class Obfuscation {
    private final String path;
    private final String pkg;
    private final HashMap<String, ArrayList<File>> filesPerDir;
    private boolean isMultiDex;
    private final ArrayList<String> smaliDirs;
    private final ArrayList<String> dexDumps;
    private int limit;
    private final ArrayList<Transformation> transformations;

    public Obfuscation(String path, String pkg) {
        transformations = new ArrayList<>();
        this.path = path;
        this.pkg = pkg;
        smaliDirs = new ArrayList<>();
        smaliDirs.add(path + SEPARATOR + "smali");
        setMultiDex();
        filesPerDir = new HashMap<>();
        for (String dir : smaliDirs) {
            filesPerDir.put(dir, searchFiles(new File(dir), ".smali", null, null));
        }
        dexDumps = new ArrayList<>();
        try {
            generateDexDump();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addJunkCodeInsertion(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        Insertion insertion;
        if (dirsToExclude != null)
            insertion = new Insertion(pathToPackage.toString(), dirsToExclude);
        else
            insertion = new Insertion(pathToPackage.toString());
        transformations.forEach(transformation -> {
            if (transformation instanceof Insertion)
                transformations.remove(transformation);
        });
        transformations.add(insertion);
    }

    public void addNopToJunk(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        NopToJunk nopToJunk;
        if (dirsToExclude != null)
            nopToJunk = new NopToJunk(pathToPackage.toString(), dirsToExclude);
        else
            nopToJunk = new NopToJunk(pathToPackage.toString());
        transformations.forEach(transformation -> {
            if (transformation instanceof NopToJunk)
                transformations.remove(transformation);
        });
        transformations.add(nopToJunk);
    }

    public void addStringEncryption(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        StringEncryption stringEncryption;
        if (dirsToExclude != null)
            stringEncryption = new StringEncryption(pathToPackage.toString(), dirsToExclude);
        else
            throw new RuntimeException("No list of directories to exclude provided when creating StringEncryption");
        transformations.forEach(transformation -> {
            if (transformation instanceof StringEncryption)
                transformations.remove(transformation);
        });
        transformations.add(stringEncryption);
    }

    public void addIdentifierRenaming(String operation) {
        Path pathToManifest = Paths.get(path, "AndroidManifest.xml");
        IdentifierRenaming idRenaming = new IdentifierRenaming(
                path, pathToManifest.toString(), Objects.requireNonNullElse(operation, "all"));
        transformations.forEach(transformation -> {
            if (transformation instanceof IdentifierRenaming)
                transformations.remove(transformation);
        });
        transformations.add(idRenaming);
    }

    public void addCodeReorder(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        CodeReorder codeReorder;
        if (dirsToExclude != null)
            codeReorder = new CodeReorder(pathToPackage.toString(), dirsToExclude);
        else
            codeReorder = new CodeReorder(pathToPackage.toString());
        transformations.forEach(transformation -> {
            if (transformation instanceof CodeReorder)
                transformations.remove(transformation);
        });
        transformations.add(codeReorder);
    }

    public void addCallIndirection(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        try {
            setLimit();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        CallIndirection callIndirection;
        if (dirsToExclude != null)
            callIndirection = new CallIndirection(pathToPackage.toString(), limit, dirsToExclude);
        else
            callIndirection = new CallIndirection(pathToPackage.toString(), limit);
        transformations.forEach(transformation -> {
            if (transformation instanceof CallIndirection)
                transformations.remove(transformation);
        });
        transformations.add(callIndirection);
    }

    public void applyTransformations() {
        transformations.forEach(transformation -> {
            try {
                transformation.obfuscate();
                System.out.println(transformation.getClass().getSimpleName() + " transformation concluded successfully");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void setLimit() throws FileNotFoundException, UnsupportedEncodingException {
        limit = 65534 - getMethodNumber();
    }

    /**
     * Determines if the project is multidex and adds the directories containing the smali files to
     * {@link Obfuscation#smaliDirs smaliDirs}
     */
    private void setMultiDex() {
        isMultiDex = false;
        Path base = Paths.get(path);
        System.out.println(base);
        int i = 2;
        Path dir;
        do {
            dir = base.resolve("smali_classes" + i);
            i++;
            isMultiDex = isMultiDex || Files.exists(dir);
            if (Files.exists(dir))
                smaliDirs.add(dir.toString());
        } while (Files.exists(dir));
    }

    /**
     * Generates the dumps of the dex files contained in the assembled APK and stores the path to them in
     * {@link Obfuscation#dexDumps dexDumps}
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void generateDexDump() throws InterruptedException, IOException {
        // TODO: implement so that this part can be run by any system
        File workingDir = new File(path + SEPARATOR + "dist");
        Path dump = Paths.get(path, "dist");
        if (!Files.exists(dump.resolve("app-release.apk")))
            throw new FileNotFoundException("app-release.apk not found in directory: " + dump);
        if (!Files.exists(dump.resolve("app-release.zip"))) {
            String[] cmd1 = {"cp", "app-release.apk", "app-release.zip"};
            if (execCommand(cmd1, workingDir) != 0)
                throw new RuntimeException("Command failed: " + String.join(" ", cmd1));
            System.out.println("cp succeeded");
        }
        if (!Files.exists(dump.resolve("decomp"))) {
            String[] cmd2 = {"unzip", "app-release.zip", "-d", "decomp"};
            if (execCommand(cmd2, workingDir) != 0)
                throw new RuntimeException("Command failed: " + String.join(" ", cmd2));
            System.out.println("unzip succeeded");
        }
        for (int i = 1; i <= smaliDirs.size(); i++) {
            String dexDumpNum = i == 1 ? "" : String.valueOf(i);
            String[] cmd3 = {"/bin/sh", "-c", "~/Library/Android/sdk/build-tools/35.0.0/dexdump -d decomp/classes" + dexDumpNum + ".dex > dump" + dexDumpNum + ".txt"};
            if (execCommand(cmd3, workingDir) != 0)
                throw new RuntimeException("Command failed: " + String.join(" ", cmd3));
            System.out.println("dexdump " + i + " succeeded");
            dexDumps.add(Paths.get(path, "dist", "dump" + dexDumpNum + ".txt").toString());
        }
        String[] cmd4 = {"rm", "app-release.zip"};
        if (execCommand(cmd4, workingDir) != 0)
            throw new RuntimeException("Command failed: " + String.join(" ", cmd4));
        System.out.println("first rm succeeded");
        String[] cmd5 = {"rm", "-r", "decomp"};
        if (execCommand(cmd5, workingDir) != 0)
            throw new RuntimeException("Command failed: " + String.join(" ", cmd5));
        System.out.println("second rm succeeded");
    }

    private int execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);
        return pr.waitFor();
    }

    /**
     * If the APK is multiDex this method counts the number of methods for every dex file
     *
     * @return HashMap containing the pairs path of dexdump and the number of methods in the files inside of it
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public HashMap<String, Integer> getMethodNumberPerDex() throws FileNotFoundException, UnsupportedEncodingException {
        return dexDumps.stream()
                .collect(Collectors.toMap(
                        dir -> dir,
                        dir -> {
                            try {
                                return countMethodsInDex(dir);
                            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        (_, replacement) -> replacement,
                        HashMap::new
                ));
    }

    /**
     * Counts the number of methods for the default dex file classes.dex
     *
     * @return the number of methods declared in the classes.dex file
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public int getMethodNumber() throws FileNotFoundException, UnsupportedEncodingException {
        return countMethodsInDex(dexDumps.getFirst());
    }

    /**
     * Counts the number of methods in a dex file
     *
     * @param dexDump path to the file containing the dexdump
     * @return the number of methods declared in the files
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public int countMethodsInDex(String dexDump) throws FileNotFoundException, UnsupportedEncodingException {
        HashSet<String> uniqueMethods = new HashSet<>();
        StringBuffer dump = getStringBufferFromFile(dexDump);
        Pattern pattern = Pattern.compile("(Class #[0-9]+)(?s)(.*?)(source_file_idx.*?\\))");
        Matcher matcher = pattern.matcher(dump);
        while (matcher.find()) {
            String classBody = matcher.group(2);
            pattern = Pattern.compile("Direct methods(?s)(.*)Virtual methods");
            Matcher matcher1 = pattern.matcher(classBody);
            String methods = "";
            if (matcher1.find()) {
                methods += matcher1.group(1);
            }
            pattern = Pattern.compile("Virtual methods(?s)(.*)");
            matcher1 = pattern.matcher(classBody);
            if (matcher1.find()) {
                methods += matcher1.group(1);
            }
            // In the dex file decompiled with the -d option, we have all methods declared in the classes contained in
            // said dex, but also all references to external libraries, whose methods are counted towards the limit of
            // 65536 methods, so the methods we need to count can be in the form of declaration or invocation; because
            // methods can be invoked more than once, we count the methods using a set, not to have duplicates
            pattern = Pattern.compile("((?s)\\(in (.*?;)\\).*?name.*?'(.*?)'.*?type.*?'(.*?)')|(invoke-.*?(L.*?;)\\.(.*?):(.*?) )");
            matcher1 = pattern.matcher(methods);
            while (matcher1.find()) {
                String method;
                if (matcher1.group(1) != null) {
                    method = matcher1.group(2) + "->" + matcher1.group(3) + matcher1.group(4);
                } else {
                    method = matcher1.group(6) + "->" + matcher1.group(7) + matcher1.group(8);
                }
                uniqueMethods.add(method);
            }
        }
        return uniqueMethods.size();
    }

}
