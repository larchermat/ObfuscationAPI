package it.unibz.obfuscationapi.Utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that performs the parsing of the log.txt files generating the trace.xes files
 */
public class LogParser {
    private static final int numLogsPerTest = 100;
    private static final HashMap<String, Integer> callsByNumberOfParams = new HashMap<>();
    private static final int nThreads = 8;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

    /**
     * Takes all logs, parses them into xes traces and produces three datasets with partial traces:
     * <ul>
     *     <li>one dataset whose traces contain 30% of the average number of events</li>
     *     <li>one dataset whose traces contain 50% of the average number of events</li>
     *     <li>one dataset whose traces contain 70% of the average number of events</li>
     * </ul>
     */
    public static void generateDatasets() {
        initializeCallsMap();
        calcAverageEventNum();
        long averageEventNum = 100;
        int[] eventsForDataset = {
                (int) Math.round(averageEventNum * 0.3),
                (int) Math.round(averageEventNum * 0.5),
                (int) Math.round(averageEventNum * 0.7),
        };
        generateTracesTraining(eventsForDataset);
        //generateTracesTesting(eventsForDataset);
        generateTracesTestingPerCategory(eventsForDataset);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(600, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /**
     * Generates all traces for a given dataset
     *
     * @param eventsForDataset number of events each trace must include
     */
    private static void generateTracesTraining(int[] eventsForDataset) {
        Path pathToDatasetDir = Paths.get("traces", "training");
        if (!pathToDatasetDir.toFile().exists()) {
            try {
                Files.createDirectories(pathToDatasetDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Path pathToLog = Paths.get("logs");
        File logsDir = pathToLog.toFile();
        ArrayList<String> logs;
        try {
            logs = new ArrayList<>(
                    navigateDirectoryContents(logsDir.getAbsolutePath(), null).stream()
                            .filter(log -> log.contains("unmodified")).toList()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < eventsForDataset.length; i++) {
            int finalI = i;
            executorService.submit(() -> generateDataset(eventsForDataset[finalI], pathToDatasetDir, finalI, logs, 0));
        }
    }

    private static void generateTrainSet(Path pathToDatasetDir, int i, ArrayList<String> logs, int numEvents) {
        OutputStreamWriter osw;
        FileOutputStream fos;
        Path pathToDataset = pathToDatasetDir.resolve("dataset" + (i + 1) + ".xes");
        try {
            fos = new FileOutputStream(pathToDataset.toString());
            osw = new OutputStreamWriter(fos);
            osw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append(LS)
                    .append("<log xmlns=\"http://www.xes-standard.org/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                    .append("xsi:schemaLocation=\"http://www.xes-standard.org/ XES.xsd\" version=\"1.0\">").append(LS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generateTrace(logs, numEvents, osw, 0);
        try {
            osw.append("</log>").append(LS);
            osw.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteDir(File apkDir) {
        File[] files = apkDir.listFiles();
        if (files != null)
            for (File file : files) {
                if (file.isDirectory()) deleteDir(file);
                else file.delete();
            }
    }

    private static void generateTracesTestingPerCategory(int[] numEvents) {
        String[] categories = {"unmodified", "AdvancedReflection", "ArithmeticBranching", "CallIndirection",
                "CodeReorder", "IdentifierRenaming", "Insertion", "NopToJunk", "StringEncryption"};
        Path pathToLog = Paths.get("logs");
        File logsDir = pathToLog.toFile();
        File[] apkDirs = logsDir.listFiles();
        ArrayList<String> logs;
        try {
            logs = navigateDirectoryContents(logsDir.getAbsolutePath(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path datasetDir = Paths.get("traces");
        for (String category : categories) {
            Path categoryDir = datasetDir.resolve(category);
            if (!Files.exists(categoryDir)) {
                try {
                    Files.createDirectories(categoryDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            ArrayList<String> categoryLogs = new ArrayList<>();
            assert apkDirs != null;
            for (File apkDir : apkDirs) {
                if (!apkDir.isDirectory()) continue;
                categoryLogs.addAll(logs.stream().filter(log -> log.contains(category) && log.contains(apkDir.getName())).limit(numLogsPerTest).toList());
            }
            for (int i = 0; i < numEvents.length; i++) {
                int finalI = i;
                executorService.submit(() -> generateDataset(numEvents[finalI], categoryDir, finalI, categoryLogs, 0));
            }
        }
    }

    private static void generateTracesTesting(int[] numEvents) {
        Path pathToLog = Paths.get("logs");
        File logsDir = pathToLog.toFile();
        File[] apkDirs = logsDir.listFiles();
        assert apkDirs != null;
        for (File apkDir : apkDirs) {
            if (!apkDir.isDirectory()) continue;
            File[] obfDirs = apkDir.listFiles();
            if (obfDirs == null) continue;
            for (File obfDir : obfDirs) {
                File[] eventDirs = obfDir.listFiles();
                if (eventDirs == null) continue;
                String obfDirPath = obfDir.getAbsolutePath();
                Path pathToDatasetDir = Paths.get(obfDirPath.replace("logs", "traces"));
                if (!pathToDatasetDir.toFile().exists()) {
                    try {
                        Files.createDirectories(pathToDatasetDir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                for (int i = 0; i < numEvents.length; i++) {
                    ArrayList<String> logs;
                    try {
                        logs = navigateDirectoryContents(obfDir.getPath(), null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (logs.isEmpty()) {
                        deleteDir(obfDir);
                        break;
                    }
                    int finalI = i;
                    executorService.submit(() -> generateDataset(numEvents[finalI], pathToDatasetDir, finalI, logs, numLogsPerTest));
                }
            }
        }
    }

    private static void generateDataset(int numEvents, Path pathToDatasetDir, int i, ArrayList<String> logs, int limit) {
        OutputStreamWriter osw;
        FileOutputStream fos;
        Path pathToDataset = pathToDatasetDir.resolve("dataset" + (i + 1) + ".xes");
        System.out.println("Thread " + Thread.currentThread() + " started");
        try {
            Files.deleteIfExists(pathToDataset);
            Files.createFile(pathToDataset);
            fos = new FileOutputStream(pathToDataset.toString());
            osw = new OutputStreamWriter(fos);
            osw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append(LS)
                    .append("<log xmlns=\"http://www.xes-standard.org/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                    .append("xsi:schemaLocation=\"http://www.xes-standard.org/ XES.xsd\" version=\"1.0\">").append(LS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean success = generateTrace(logs, numEvents, osw, limit);
        try {
            osw.append("</log>").append(LS);
            osw.flush();
            System.out.println("Thread " + Thread.currentThread() + " finished");
            osw.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!success) {
            try {
                Files.delete(pathToDataset);
                System.out.println("Deleted " + pathToDataset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Calculates the average number of events taking into account all produced logs and counting the lines (one line in
     * a log is a system call, which is translated into a single event when parsing)
     *
     * @return the average number of events per execution
     */
    private static long calcAverageEventNum() {
        Path path = Paths.get("logs");
        ArrayList<String> logs;
        ArrayList<Integer> eventNums = new ArrayList<>();
        try {
            logs = Utilities.navigateDirectoryContents(path.toString(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<Path> logsToDelete = new ArrayList<>();
        for (String log : logs) {
            int eventNum = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
                while (reader.readLine() != null) {
                    eventNum++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (eventNum <= 70) {
                logsToDelete.add(Paths.get(log));
                continue;
            }
            eventNums.add(eventNum);
        }
        for (Path log : logsToDelete) {
            try {
                System.out.println("Deleting " + log);
                Files.delete(log);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Math.round(eventNums.stream().mapToInt(Integer::intValue).average().orElse(0.0));
    }

    private static void initializeCallsMap() {
        Path path = Paths.get("logs");
        ArrayList<String> logs;
        try {
            logs = navigateDirectoryContents(path.toString(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<Future<?>> tasks = new ArrayList<>();
        int size = logs.size() / nThreads;
        for (int i = 0; i < nThreads; i++) {
            int start = i * size;
            int end = i == (nThreads - 1) ? logs.size() : (i + 1) * size;
            tasks.add(executorService.submit(() -> parseLogCalls(new ArrayList<>(logs.subList(start, end)))));
        }
        boolean cond;
        do {
            cond = tasks.stream().allMatch(Future::isDone);
        } while (!cond);
    }

    private static void parseLogCalls(ArrayList<String> logs) {
        ArrayList<Event> events = new ArrayList<>();
        for (String log : logs) {
            StringBuffer content;
            try {
                content = getStringBufferFromFile(log);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            Pattern pattern = Pattern.compile("(?:\\s+([0-9]+\\.[0-9]+) )?" +
                    "(([a-zA-Z_0-9]+)\\((.*?)\\)\\s+= (.*?)(?: <([0-9]+\\.[0-9]+)>)?)");
            Matcher matcher = pattern.matcher(content.toString());
            while (matcher.find()) {
                Event event = new Event(matcher, 0);
                if (event.type.equals("error"))
                    continue;
                events.add(event);
            }
            content.delete(0, content.length());
            if (events.size() > 100) {
                for (Event event : events) {
                    synchronized (callsByNumberOfParams) {
                        if (callsByNumberOfParams.containsKey(event.call)) {
                            if (event.arguments.size() > callsByNumberOfParams.get(event.call)) {
                                callsByNumberOfParams.replace(event.call, event.arguments.size());
                            }
                        } else {
                            callsByNumberOfParams.put(event.call, event.arguments.size());
                        }
                    }
                }
            }
            events.clear();
        }
        for (Event event : events) {
            synchronized (callsByNumberOfParams) {
                if (callsByNumberOfParams.containsKey(event.call)) {
                    if (event.arguments.size() > callsByNumberOfParams.get(event.call)) {
                        callsByNumberOfParams.replace(event.call, event.arguments.size());
                    }
                } else {
                    callsByNumberOfParams.put(event.call, event.arguments.size());
                }
            }
        }
    }

    /**
     * This method takes the list of the log paths and the path to their parent directory.
     * It then processes them one by one extracting the information about calls, converting it into events that are
     * appended to the OutputStreamWriter passed as parameter.
     *
     * @param logs      list of the paths of the log.txt files
     * @param numEvents number of events for each trace
     * @param osw       output stream pointing to the dataset we are appending the traces to
     */
    private static boolean generateTrace(ArrayList<String> logs, int numEvents, OutputStreamWriter osw, int limit) {
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        int tracesAdded = 0;
        for (String log : logs) {
            if (limit != 0 && tracesAdded >= limit)
                break;
            StringBuffer contents;
            try {
                contents = getStringBufferFromFile(log);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            // We have two cases, either a call, or an error
            Pattern pattern = Pattern.compile("(?:\\s+([0-9]+\\.[0-9]+) )?" +
                    "(?:(([a-zA-Z_0-9]+)\\((.*?)\\)\\s+= (.*?)(?: <([0-9]+\\.[0-9]+)>)?)|" +
                    "(--- (.*?) \\{(.*?)} ---))" + LS);
            Matcher matcher = pattern.matcher(contents.toString());
            ArrayList<Event> events = new ArrayList<>();
            int parsedEvents = 0;
            double eventTimestamp = 0.0;
            while (matcher.find() && parsedEvents < numEvents) {
                start += Math.round(eventTimestamp * 1000);
                Event event = new Event(matcher, start);
                if (event.type.equals("error")) {
                    continue;
                }
                events.add(event);
                parsedEvents++;
                eventTimestamp = Double.parseDouble(matcher.group(1) == null ? "0" : matcher.group(1));
            }
            if (parsedEvents != numEvents)
                continue;
            sb.append(getTraceHeader(log, (tracesAdded + 1)));
            for (Event event : events) {
                sb.append(TAB).append(TAB).append("<event>").append(LS);
                sb.append(event.getEvent());
                sb.append(TAB).append(TAB).append("</event>").append(LS).append(LS);
                if (sb.length() > 524288) {
                    try {
                        osw.append(sb.toString());
                        osw.flush();
                        sb.delete(0, sb.length());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            sb.append(TAB).append("</trace>").append(LS);
            tracesAdded++;
        }
        try {
            osw.append(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tracesAdded != 0;
    }

    /**
     * Parses the path to the log to generate the trace header for the current execution
     *
     * @param path path to the log
     * @return string containing the trace header
     */
    private static String getTraceHeader(String path, int execNumber) {
        path = path.substring(path.indexOf("logs"));
        Scanner scanner = new Scanner(path);
        scanner.useDelimiter(SEPARATOR);
        scanner.next();
        return TAB + "<trace>" + LS +
                TAB + TAB + "<string key=\"concept:name\" value=\"Execution_" + execNumber + "\"/>" + LS +
                TAB + TAB + "<string key=\"label\" value=\"" + scanner.next() + "\"/>" + LS + LS;
    }

    /**
     * This class contains the information of a system call contained in a log file, and is used to rewrite it in the
     * format of an event
     */
    private static class Event {
        String type;
        String call;
        String error;
        ArrayList<String> arguments;
        String ret;
        long timestamp;

        public Event(Matcher matcher, double timestamp) {
            this.timestamp = (long) timestamp;
            arguments = new ArrayList<>();
            if (matcher.group(2) != null) {
                type = "call";
                call = sanitize(matcher.group(3));
                Scanner scanner = new Scanner(matcher.group(4) == null ? "" : sanitize(matcher.group(4)));
                scanner.useDelimiter(",");
                while (scanner.hasNext()) {
                    StringBuilder argument = new StringBuilder(scanner.next());
                    if ((argument.toString().contains("(") && !argument.toString().contains(")")) ||
                            (argument.toString().contains("[") && !argument.toString().contains("]")) ||
                            (argument.toString().contains("{") && !argument.toString().contains("}"))) {
                        while (scanner.hasNext() && !closedBrackets(argument.toString())) {
                            argument.append(scanner.next());
                        }
                    }
                    arguments.add(argument.toString().strip());
                }
                ret = sanitize(matcher.group(5));
            } else if (matcher.group(7) != null) {
                type = "error";
                error = sanitize(matcher.group(8));
                Scanner scanner = new Scanner(matcher.group(9) == null ? "" : sanitize(matcher.group(9)));
                scanner.useDelimiter(",");
                while (scanner.hasNext()) {
                    StringBuilder argument = new StringBuilder(scanner.next());
                    if ((argument.toString().contains("(") && !argument.toString().contains(")")) ||
                            (argument.toString().contains("[") && !argument.toString().contains("]")) ||
                            (argument.toString().contains("{") && !argument.toString().contains("}"))) {
                        while (scanner.hasNext() && !closedBrackets(argument.toString())) {
                            argument.append(scanner.next());
                        }
                    }
                    arguments.add(argument.toString().strip());
                }
            } else {
                throw new RuntimeException("Unknown event: " + matcher.group());
            }
        }

        public String getEvent() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = new Date(timestamp);
            String timeString = dateFormat.format(date).replace(" ", "T") + "+02:00";
            StringBuilder args = new StringBuilder();
            StringBuilder rets = new StringBuilder();
            for (String call : callsByNumberOfParams.keySet()) {
                if (type.equals("error"))
                    continue;
                if (this.call.equals(call)) {
                    for (int i = 0; i < arguments.size(); i++) {
                        String argument = arguments.get(i);
                        args.append("" + TAB + TAB + TAB).append("<string key=\"param_").append(i + 1).append("_").append(call)
                                .append("\" value=\"").append(argument)
                                .append("\"/>").append(LS);
                    }
                    for (int i = 0; i < (callsByNumberOfParams.get(call) - arguments.size()); i++) {
                        args.append("" + TAB + TAB + TAB).append("<string key=\"param_").append(arguments.size() + i + 1).append("_").append(call)
                                .append("\" value=\"NULL\"/>").append(LS);
                    }
                    rets.append(TAB + TAB + TAB + "<string key=\"return_").append(call).append("\" value=\"")
                            .append(ret).append("\"/>").append(LS);
                } else {
                    for (int i = 0; i < callsByNumberOfParams.get(call); i++) {
                        args.append("" + TAB + TAB + TAB).append("<string key=\"param_").append(i + 1).append("_").append(call)
                                .append("\" value=\"NULL\"/>").append(LS);
                    }
                    rets.append(TAB + TAB + TAB + "<string key=\"return_").append(call)
                            .append("\" value=\"NULL\"/>").append(LS);
                }
            }
            if (type.equals("call")) {
                return "" + TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + call + "\"/>" + LS +
                        args +
                        TAB + TAB + TAB + "<date key=\"time:timestamp\" value=\"" + timeString + "\"/>" + LS +
                        rets;
            } else {
                return ""; //+ TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + error + "\"/>" + LS +
                //args;
            }
        }

        /**
         * Method to sanitize the string escaping the reserved characters of xml
         *
         * @param string string to sanitize
         * @return the sanitized string
         */
        private String sanitize(String string) {
            return string.replace("&", "&amp;").replace("\"", "&quot;")
                    .replace("'", "&apos;").replace("<", "&lt;")
                    .replace(">", "&gt;");
        }

        private boolean closedBrackets(String string) {
            int round = 0;
            int square = 0;
            int curly = 0;
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) == '(')
                    round += 1;
                if (string.charAt(i) == ')' && round > 0)
                    round -= 1;
                if (string.charAt(i) == '[')
                    square += 1;
                if (string.charAt(i) == ']' && square > 0)
                    square -= 1;
                if (string.charAt(i) == '{')
                    curly += 1;
                if (string.charAt(i) == '}' && curly > 0)
                    curly -= 1;
            }
            return round == 0 && square == 0 && curly == 0;
        }
    }
}