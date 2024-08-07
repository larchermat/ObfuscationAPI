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
    private String pkg;
    private boolean isMultiDex;
    private final ArrayList<String> smaliDirs;
    private final ArrayList<String> dexDumps;
    private int limit;
    private final ArrayList<Transformation> transformations;
    private final String os;
    private final String appName;

    public Obfuscation() throws IOException, InterruptedException {
        transformations = new ArrayList<>();
        System.out.println("Insert path to the APK (including the .apk file with extension)");
        Scanner scanner = new Scanner(System.in);
        String pathToApk = scanner.nextLine();
        appName = pathToApk.substring(pathToApk.lastIndexOf(SEPARATOR) + 1);
        os = System.getProperty("os.name").toLowerCase();
        decompileAPK(pathToApk);
        path = Paths.get("decompiled").toString();
        setPkg();
        smaliDirs = new ArrayList<>();
        smaliDirs.add(path + SEPARATOR + "smali");
        dexDumps = new ArrayList<>();
        setMultiDex();
    }

    /**
     * Executes the scripts in the scripts folder to decompile the APK and generate the dumps of the dex files
     *
     * @param pathToApk path to the original APK to decompile
     * @throws IOException
     * @throws InterruptedException
     */
    private void decompileAPK(String pathToApk) throws IOException, InterruptedException {
        // TODO: add the repo initialization to the bash and cmd scripts
        if (os.contains("win")) {
            File file = new File(Paths.get("scripts", "win").toString());
            String[] cmd = {"cmd.exe", "/c", "decompileAPK.cmd", pathToApk};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Windows command \"" + String.join("", cmd) + "\" failed");
        } else if (os.contains("mac")) {
            File file = new File(Paths.get("scripts", "unix", "mac").toString());
            String[] cmd = {"bash", "decompileAPK.sh", pathToApk};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Mac command \"" + String.join("", cmd) + "\" failed");
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            File file = new File(Paths.get("scripts", "unix", "linux").toString());
            String[] cmd = {"bash", "decompileAPK.sh", pathToApk};
            if (execCommand(cmd, file) != 0)
                throw new RuntimeException("Linux command \"" + String.join("", cmd) + "\" failed");
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
    public void rebuildAPK() throws IOException, InterruptedException {
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

    private int execCommand(String[] args, File file) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(args, null, file);
        return pr.waitFor();
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
        HashMap<String, Integer> dirsByLimit;
        try {
            dirsByLimit = getSmaliDirsByMethodLimit();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            ArrayList<String> dirsOrder = new ArrayList<>(dirsByLimit.keySet());
            int i = 0;
            for (String dir : dirsOrder) {
                i++;
                System.out.println("[" + i + "] Number of methods that can be added to " + dir + ": " + dirsByLimit.get(dir));
            }
            System.out.println("Do you want to change these settings? [yes/no]");
            String res = scanner.nextLine();
            if (res.equalsIgnoreCase("no"))
                break;
            System.out.println("Which setting do you want to change? [1-" + i + "]");
            int setting = scanner.nextInt();
            scanner.nextLine();
            while (setting < 1 || setting > i) {
                System.out.println("Please enter a number between 1 and " + (i + 1) + ": ");
                setting = scanner.nextInt();
                scanner.nextLine();
            }
            int oldLimit = dirsByLimit.get(dirsOrder.get(setting - 1));
            System.out.println("How many methods would you like to add? [0-" + oldLimit + "]");
            int newLimit = scanner.nextInt();
            scanner.nextLine();
            while (newLimit < 0 || newLimit > oldLimit) {
                System.out.println("Please enter a number between 0 and " + oldLimit + ": ");
                newLimit = scanner.nextInt();
                scanner.nextLine();
            }
            dirsByLimit.put(dirsOrder.get(setting - 1), newLimit);
        }
        CallIndirection callIndirection;
        if (dirsToExclude != null)
            callIndirection = new CallIndirection(dirsByLimit, dirsToExclude);
        else
            callIndirection = new CallIndirection(dirsByLimit);
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

    private void setPkg() {
        String pathToManifest = Paths.get(path, "AndroidManifest.xml").toString();
        StringBuffer sb;
        try {
            sb = getStringBufferFromFile(pathToManifest);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Pattern pattern = Pattern.compile("(package=\")([a-zA-Z0-9.]*\\.)([a-zA-Z0-9]*)(\")");
        Matcher matcher = pattern.matcher(sb.toString());
        if (matcher.find())
            pkg = (matcher.group(2) + matcher.group(3)).replace(".", "/");
    }

    /**
     * Determines if the project is multidex and adds the directories containing the smali files to
     * {@link Obfuscation#smaliDirs smaliDirs} and the dumps of the dex files to {@link Obfuscation#dexDumps dexDumps}
     */
    private void setMultiDex() {
        isMultiDex = false;
        Path base = Paths.get(path);
        int i = 2;
        Path dir;
        do {
            dir = base.resolve("smali_classes" + i);
            i++;
            isMultiDex = isMultiDex || Files.exists(dir);
            if (Files.exists(dir))
                smaliDirs.add(dir.toString());
        } while (Files.exists(dir));
        Path dexDump = Paths.get("");
        for (i = 1; i <= smaliDirs.size(); i++) {
            dexDumps.add(dexDump.resolve("dump" + i + ".txt").toString());
        }
    }

    /**
     * If the APK is multiDex this method counts the number of methods for every dex file
     *
     * @return HashMap containing the pairs path of dexdump and the number of methods in the files inside of it
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public HashMap<String, Integer> getSmaliDirsByMethodLimit() throws FileNotFoundException, UnsupportedEncodingException {
        return dexDumps.stream()
                .collect(Collectors.toMap(
                        dir -> smaliDirs.get(dexDumps.indexOf(dir)),
                        dir -> {
                            try {
                                return 65534 - countMethodsInDex(dir);
                            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        (_, replacement) -> replacement,
                        HashMap::new
                ));
    }

    /**
     * Counts all the methods inside a dex file
     * Because dex files can be huge, storing a StringBuffer in memory with the whole content of the file could be
     * detrimental to the speed of execution of the program if it occupies too much space; to avoid this, the dex file
     * contents are read sequentially and analyzed one at a time
     * With the current chunkSize we allocate 2MB of memory for the buffer to read from, and the StringBuilder will be
     * around the same size, so we're occupying in total around 4MB of memory with the objects in this method
     *
     * @param dexDump string containing the path to the dex file
     * @return
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public int countMethodsInDex(String dexDump) throws FileNotFoundException, UnsupportedEncodingException {
        HashSet<String> uniqueMethods = new HashSet<>();
        Pattern pattern = Pattern.compile("(Class #[0-9]+)(?s)(.*?)(source_file_idx.*?\\))");
        try (BufferedReader reader = new BufferedReader(new FileReader(dexDump))) {
            int chunkSize = 2097152;
            char[] buffer = new char[chunkSize];
            StringBuilder sb = new StringBuilder();
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, chunkSize)) != -1) {
                sb.append(buffer, 0, charsRead);
                Matcher matcher;
                // If the matcher does not find a match, then it means we might have a truncated match, so we continue
                // reading until we find the match, or if we get to the end, it means that there is nothing left of
                // interest to read
                while (true) {
                    matcher = pattern.matcher(sb.toString());
                    if (matcher.find()) {
                        break;
                    } else {
                        if ((charsRead = reader.read(buffer, 0, chunkSize)) != -1) {
                            sb.append(buffer, 0, charsRead);
                        } else {
                            // If the matcher could not find the class regex, and we have finished reading the dex file
                            // it means that in the rest of the file there are no more useful information for us
                            return uniqueMethods.size();
                        }
                    }
                }

                countMethodsInDexChunk(sb, uniqueMethods);
                int end = 0;
                while (matcher.find())
                    end = matcher.end();
                // We need to ensure not to delete a truncated match inside the StringBuilder because if so, the
                // information would get lost in the next iteration; because of this we delete everything until the end
                // of the last match and keep the last part
                sb.delete(0, end);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uniqueMethods.size();
    }
    // 131072 is 16 and 817
    // 262144 is 14 and 113
    // 524288 is 9 and 639
    // 1048576 is 7 and 12
    // 2097152 is 5 and 649


    /**
     * Updates the HashSet provided adding all unique methods found in a chunk of the dex file
     *
     * @param dexChunk StringBuilder containing the chunk of the dex file to analyze
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void countMethodsInDexChunk(StringBuilder dexChunk, HashSet<String> uniqueMethods) throws FileNotFoundException, UnsupportedEncodingException {
        Pattern pattern = Pattern.compile("(Class #[0-9]+)(?s)(.*?)(source_file_idx.*?\\))");
        Matcher matcher = pattern.matcher(dexChunk);
        while (matcher.find()) {
            String classBody = matcher.group(2);
            Pattern pattern1 = Pattern.compile("Direct methods(?s)(.*)Virtual methods");
            Matcher matcher1 = pattern1.matcher(classBody);
            String methods = "";
            if (matcher1.find()) {
                classBody = classBody.substring(matcher1.start());
                methods += matcher1.group(1);
            }
            pattern1 = Pattern.compile("Virtual methods(?s)(.*)");
            matcher1 = pattern1.matcher(classBody);
            if (matcher1.find()) {
                methods += matcher1.group(1);
            }
            // In the dex file decompiled with the -d option, we have all methods declared in the classes contained in
            // said dex, but also all references to external libraries, whose methods are counted towards the limit of
            // 65536 methods, so the methods we need to count can be in the form of declaration or invocation; because
            // methods can be invoked more than once, we count the methods using a set, not to have duplicates
            pattern1 = Pattern.compile("((?s)\\(in (.*?;)\\).*?name.*?'(.*?)'.*?type.*?'(.*?)')|(invoke-.*?(L.*?;)\\.(.*?):(.*?) )");
            matcher1 = pattern1.matcher(methods);
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
    }

}
