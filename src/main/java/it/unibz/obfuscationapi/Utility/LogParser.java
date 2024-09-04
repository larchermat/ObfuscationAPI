package it.unibz.obfuscationapi.Utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that performs the parsing of the log.txt files generating the trace.xes files
 */
public class LogParser {
    public static long start = System.currentTimeMillis();

    /**
     * Takes all logs, parses them into xes traces and produces three datasets with partial traces:
     * <ul>
     *     <li>one dataset whose traces contain 30% of the average number of events</li>
     *     <li>one dataset whose traces contain 50% of the average number of events</li>
     *     <li>one dataset whose traces contain 70% of the average number of events</li>
     * </ul>
     */
    public static void generateDatasets() {
        long averageEventNum = calcAverageEventNum();
        long[] eventsForDataset = {
                Math.round(averageEventNum * 0.3),
                Math.round(averageEventNum * 0.5),
                Math.round(averageEventNum * 0.7)
        };
        for (int i = 0; i < eventsForDataset.length; i++) {
            Path pathToDataset = Paths.get("traces");
            try {
                if (!pathToDataset.toFile().exists()) {
                    Files.createDirectories(pathToDataset);
                }
                pathToDataset = pathToDataset.resolve("dataset" + (i + 1) + ".xes");
                Files.deleteIfExists(pathToDataset);
                Files.createFile(pathToDataset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            generateTraces(eventsForDataset[i], pathToDataset);
        }
    }

    /**
     * Generates all traces for a given dataset
     *
     * @param numEvents     number of events each trace must include
     * @param pathToDataset path to the dataset file
     */
    private static void generateTraces(long numEvents, Path pathToDataset) {
        FileOutputStream fos;
        OutputStreamWriter osw;
        try {
            fos = new FileOutputStream(pathToDataset.toString());
            osw = new OutputStreamWriter(fos);
            osw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append(LS)
                    .append("<log xmlns=\"http://www.xes-standard.org/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                    .append("xsi:schemaLocation=\"http://www.xes-standard.org/ XES.xsd\" version=\"1.0\">").append(LS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path pathToLog = Paths.get("logs");
        File logsDir = pathToLog.toFile();
        File[] apkDirs = logsDir.listFiles();
        assert apkDirs != null;
        for (File apkDir : apkDirs) {
            if (!apkDir.isDirectory()) continue;
            File[] obfDirs = apkDir.listFiles();
            if (obfDirs == null) continue;
            for (File obfDir : obfDirs) {
                if (!obfDir.isDirectory()) continue;
                File[] eventDirs = obfDir.listFiles();
                if (eventDirs == null) continue;
                for (File eventDir : eventDirs) {
                    if (!eventDir.isDirectory()) continue;
                    File[] logFiles = eventDir.listFiles();
                    if (logFiles == null) continue;
                    ArrayList<String> logs = new ArrayList<>(Arrays.stream(logFiles).map(File::getPath).toList());
                    // For the traces generated we include the time of execution for the sake of determining how
                    // much time passes in between calls. We can take the current time as the starting point of the
                    // executions because the date and hour are irrelevant, we just need the intervals
                    start = System.currentTimeMillis();
                    generateTrace(logs, numEvents, osw);
                }
            }
        }
        try {
            osw.append("</log>").append(LS);
            osw.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        for (String log : logs) {
            int eventNum = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
                while (reader.readLine() != null) {
                    eventNum++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            eventNums.add(eventNum);
        }
        return Math.round(eventNums.stream().mapToInt(Integer::intValue).average().orElse(0.0));
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
    private static void generateTrace(ArrayList<String> logs, long numEvents, OutputStreamWriter osw) {
        StringBuilder sb = new StringBuilder();
        logs.sort(String::compareTo);
        for (String log : logs) {
            // We consider the executions as if they are subsequent by keeping the total time passed since the start
            // time
            double eventTimestamp = 0.0;
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
            while (matcher.find() && parsedEvents < numEvents) {
                eventTimestamp += Double.parseDouble(matcher.group(1) == null ? "0" : matcher.group(1));
                Event event = new Event(matcher, eventTimestamp);
                events.add(event);
                parsedEvents++;
            }
            if (parsedEvents != numEvents)
                continue;
            start += Math.round(eventTimestamp * 1000);
            sb.append(getTraceHeader(log));
            for (Event event : events) {
                sb.append(TAB).append(TAB).append("<event>").append(LS);
                sb.append(event.getEvent());
                sb.append(TAB).append(TAB).append("</event>").append(LS).append(LS);
                if (sb.length() > 2097152) {
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
        }
        try {
            osw.append(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the path to the log to generate the trace header for the current execution
     *
     * @param path path to the log
     * @return string containing the trace header
     */
    private static String getTraceHeader(String path) {
        path = path.substring(path.indexOf("logs"));
        Scanner scanner = new Scanner(path);
        scanner.useDelimiter(SEPARATOR);
        scanner.next();
        Pattern pattern = Pattern.compile("log([0-9]+)\\.txt");
        Matcher matcher = pattern.matcher(path);
        int i;
        if (matcher.find()) {
            i = Integer.parseInt(matcher.group(1));
        } else {
            throw new RuntimeException("Invalid path");
        }
        return TAB + "<trace>" + LS +
                TAB + TAB + "<string key=\"concept:name\" value=\"" + scanner.next() + "\"/>" + LS +
                TAB + TAB + "<string key=\"execution:id\" value=\"Execution_" + i + "\"/>" + LS + LS;
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

        public Event(Matcher matcher, double timeFromStart) {
            timestamp = start + Math.round(timeFromStart * 1000);
            arguments = new ArrayList<>();
            if (matcher.group(2) != null) {
                type = "call";
                call = sanitize(matcher.group(3));
                Scanner scanner = new Scanner(matcher.group(4) == null ? "" : sanitize(matcher.group(4)));
                scanner.useDelimiter(",");
                while (scanner.hasNext()) {
                    arguments.add(scanner.next());
                }
                ret = sanitize(matcher.group(5));
            } else if (matcher.group(7) != null) {
                type = "error";
                error = sanitize(matcher.group(8));
                Scanner scanner = new Scanner(matcher.group(9) == null ? "" : sanitize(matcher.group(9)));
                scanner.useDelimiter(",");
                while (scanner.hasNext()) {
                    arguments.add(scanner.next());
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
            for (int i = 0; i < arguments.size(); i++) {
                String argument = arguments.get(i);
                args.append("" + TAB + TAB + TAB).append("<string key=\"argument_").append(i + 1).append("_").append(call)
                        .append("\" value=\"").append(argument)
                        .append("\"/>").append(LS);
            }
            if (type.equals("call")) {
                return "" + TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + call + "\"/>" + LS +
                        args +
                        TAB + TAB + TAB + "<date key=\"time:timestamp\" value=\"" + timeString + "\"/>" + LS +
                        TAB + TAB + TAB + "<string key=\"return_" + call + "\" value=\"" + ret + "\"/>" + LS;
            } else {
                return "" + TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + error + "\"/>" + LS +
                        args;
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
    }
}