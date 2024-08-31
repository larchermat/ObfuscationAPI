package it.unibz.obfuscationapi.Obfuscation;

import it.unibz.obfuscationapi.AdvancedReflection.AdvancedReflection;
import it.unibz.obfuscationapi.ArithmeticBranching.ArithmeticBranching;
import it.unibz.obfuscationapi.CallIndirection.CallIndirection;
import it.unibz.obfuscationapi.CodeReorder.CodeReorder;
import it.unibz.obfuscationapi.Events.EventCommandFactory;
import it.unibz.obfuscationapi.Events.EventType;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.unibz.obfuscationapi.Obfuscation.CommandExecution.*;
import static it.unibz.obfuscationapi.Utility.Utilities.*;
import static java.lang.Thread.currentThread;

public class Obfuscation {
    private final String path;
    private String pkg;
    private boolean isMultiDex;
    private final ArrayList<String> smaliDirs;
    private final ArrayList<String> dexDumps;
    private final ArrayList<Transformation> transformations;
    //private ArrayList<String> permissionsList;
    private final String appName;
    private String mainActivity;
    public final ArrayList<String> avds = new ArrayList<>();
    public final HashMap<String, Boolean> avdsByAvailability = new HashMap<>();
    public final HashMap<Integer, Boolean> portsByAvailability = new HashMap<>();
    public final HashMap<String, Integer> logsByNumber = new HashMap<>();

    public Obfuscation(String pathToApk, int numAvds, String avdName) throws IOException, InterruptedException {
        transformations = new ArrayList<>();
        appName = pathToApk.substring(pathToApk.lastIndexOf(SEPARATOR) + 1).replace(".apk", "");
        decompileAPK(pathToApk, appName);
        path = Paths.get("decompiled", appName).toString();
        setPkg();
        smaliDirs = new ArrayList<>();
        smaliDirs.add(path + SEPARATOR + "smali");
        dexDumps = new ArrayList<>();
        setMultiDex();
        avds.add(avdName);
        for (int i = 2; i <= numAvds; i++) {
            avds.add(avdName + "_" + i);
        }
        int port = 5554;
        synchronized (portsByAvailability) {
            for (int i = 0; i < numAvds; i++) {
                portsByAvailability.put(port, Boolean.TRUE);
                port += 2;
            }
        }
    }

    public void startSampling() {
        ExecutorService executorService = Executors.newFixedThreadPool(avds.size());
        for (String avd : avds) {
            executorService.submit(() -> initDevice(avd));
        }
        addCallIndirection(null);
        addJunkCodeInsertion(null);
        addArithmeticBranching(null);
        addCodeReorder(null);
        addIdentifierRenaming(null);
        addAdvancedApiReflection(null);
        addNopToJunk(null);
        for (Transformation t : transformations) {
            executorService.submit(() -> applyTransformation(t));
        }
        for (int i = 0; i < avds.size(); i++) {
            for (Transformation t : transformations) {
                Runner runner = new Runner(t);
                executorService.submit(runner);
            }
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void initDevice(String avd) {
        int port = findAvailablePort();
        try {
            prepareDevice(avd, port);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (avdsByAvailability) {
                avdsByAvailability.put(avd, Boolean.TRUE);
            }
            synchronized (portsByAvailability) {
                portsByAvailability.put(port, Boolean.TRUE);
            }
        }
    }

    private int findAvailablePort() {
        int port = 0;
        while (port == 0) {
            synchronized (portsByAvailability) {
                for (Integer p : portsByAvailability.keySet()) {
                    if (portsByAvailability.get(p)) {
                        portsByAvailability.put(p, Boolean.FALSE);
                        port = p;
                        break;
                    }
                }
            }
        }
        return port;
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
            stringEncryption = new StringEncryption(pathToPackage.toString());
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
        Path pathToSmali = Paths.get(smaliDirs.getFirst(), pkg);
        CodeReorder codeReorder;
        if (dirsToExclude != null)
            codeReorder = new CodeReorder(pathToSmali.toString(), dirsToExclude);
        else
            codeReorder = new CodeReorder(pathToSmali.toString());
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

    public void addArithmeticBranching(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        ArithmeticBranching arithmeticBranching;
        if (dirsToExclude != null)
            arithmeticBranching = new ArithmeticBranching(pathToPackage.toString(), dirsToExclude);
        else
            arithmeticBranching = new ArithmeticBranching(pathToPackage.toString());
        transformations.forEach(transformation -> {
            if (transformation instanceof ArithmeticBranching)
                transformations.remove(transformation);
        });
        transformations.add(arithmeticBranching);
    }

    public void addAdvancedApiReflection(ArrayList<String> dirsToExclude) {
        Path pathToPackage = Paths.get(smaliDirs.getFirst(), pkg);
        AdvancedReflection advancedReflection;
        if (dirsToExclude != null)
            advancedReflection = new AdvancedReflection(pathToPackage.toString(), dirsToExclude);
        else
            advancedReflection = new AdvancedReflection(pathToPackage.toString());
        transformations.forEach(transformation -> {
            if (transformation instanceof AdvancedReflection)
                transformations.remove(transformation);
        });
        transformations.add(advancedReflection);
    }

    synchronized public void applyTransformation(Transformation transformation) {
        try {
            System.out.println(currentThread().getName() + " started " + transformation.getClass().getSimpleName() + " transformation");
            transformation.obfuscate();
            System.out.println(transformation.getClass().getSimpleName() + " transformation concluded successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            buildAPK(transformation.getClass().getSimpleName());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public void buildAPK(String transformation) throws IOException, InterruptedException {
        rebuildAPK(appName, transformation);
    }

    /**
     * Installs the APK on the emulated device and runs the application collecting the log of the execution after having
     * sent an activity event
     *
     * @throws IOException
     */
    public void runApk(EventType eT, String transformation) throws IOException {
        Path pathToLogs = Paths.get("logs", appName, transformation, eT.toString());
        Files.createDirectories(pathToLogs);
        String AE = EventCommandFactory.getCommand(eT).getCommand();
        int i = 1;
        synchronized (logsByNumber) {
            if (logsByNumber.containsKey(pathToLogs.toString()))
                i = logsByNumber.get(pathToLogs.toString());
            if (i > 1)
                return;
            logsByNumber.put(pathToLogs.toString(), (i + 1));
        }

        String avd = null;
        while (avd == null) {
            synchronized (avdsByAvailability) {
                for (String device : avdsByAvailability.keySet()) {
                    if (avdsByAvailability.get(device)) {
                        avdsByAvailability.put(device, Boolean.FALSE);
                        avd = device;
                        break;
                    }
                }
            }
        }
        int port = findAvailablePort();
        String pkg;
        String mainActivity;
        if (transformation.equals("IdentifierRenaming")) {
            IdentifierRenaming idRenaming = (IdentifierRenaming) transformations.stream()
                    .filter(t -> t instanceof IdentifierRenaming).findFirst().get();
            pkg = idRenaming.modifiedPkgName;
            mainActivity = "/." + idRenaming.newMainClassName;
        } else {
            pkg = this.pkg;
            mainActivity = this.mainActivity;
        }
        String pathToApk = appName + SEPARATOR + "dist" + SEPARATOR + transformation + SEPARATOR + appName;
        System.out.println(currentThread() + " is running device " + avd + " on port " + port);
        try {
            installAPK(pathToApk, avd, port/*, pkg.replace("/", "."), permissionsList*/);
            String pathToLogFile = pathToLogs.resolve("log" + i + ".txt").toAbsolutePath().toString();
            generateLog(pkg.replace("/", "."), mainActivity, pathToLogFile, port, AE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (avdsByAvailability) {
                avdsByAvailability.put(avd, Boolean.TRUE);
            }
            synchronized (portsByAvailability) {
                portsByAvailability.put(port, Boolean.TRUE);
            }
        }
    }

    /**
     * Sets the package name, the entrypoint (main activity) of the application and the required permissions (if any)
     * inspecting the AndroidManifest.xml file
     */
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
        else
            throw new RuntimeException("Could not find package in AndroidManifest.xml");
        pattern = Pattern.compile("<activity.*?android:name=\"(?:" + pkg.replace("/", "\\.") + ")?(.*?)\"");
        matcher = pattern.matcher(sb.toString());
        if (matcher.find()) {
            mainActivity = "/" + (matcher.group(1).startsWith(".") ? matcher.group(1) : "." + matcher.group(1));
        }
        else
            throw new RuntimeException("Could not find main activity in AndroidManifest.xml");

//        pattern = Pattern.compile("<uses-permission android:name=\"android\\.permission\\.(.*?)\"");
//        matcher = pattern.matcher(sb.toString());
//        permissionsList = new ArrayList<>();
//        while (matcher.find()) {
//            permissionsList.add(matcher.group(1));
//        }
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
        Path dexDump = Paths.get("decompiled", appName);
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
                        dir -> smaliDirs.get(dexDumps.indexOf(dir)) + SEPARATOR + pkg.replace("/", SEPARATOR),
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

    private class Runner implements Runnable {
        String transformation;

        public Runner(Transformation transformation) {
            this.transformation = transformation.getClass().getSimpleName();
        }

        @Override
        public void run() {
            for (EventType eventType : EventType.values()) {
                for (int i = 0; i < 1; i++) {
                    try {
                        runApk(eventType, transformation);
                    } catch (IOException | RuntimeException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
