package it.unibz.obfuscationapi.Utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that performs the parsing of the log.txt files generating the trace.xes files
 */
public class LogParser {
    public static long start = System.currentTimeMillis();

    /**
     * The method looks through the log folder, fetching the log.txt files contained generated while running the same
     * APK, with the same obfuscation technique and sending the same activity event
     */
    public static void generateTraces() {
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
                    generateTrace(logs, eventDir.getPath());
                }
            }
        }
    }

    /**
     * This method takes the list of the log paths and the path to their parent directory.
     * It then processes them one by one extracting the information about calls, converting it into events that are
     * inserted in a trace.xes file located under the same path as the logs, but starting from the traces folder and not
     * the logs folder in the root directory.
     * Each log.txt will become a trace entity in the generated trace.xes file, and they will be named "Execution n",
     * where n is the number of the log
     * @param logs list of the paths of the log.txt files
     * @param path parent directory of the logs
     */
    private static void generateTrace(ArrayList<String> logs, String path) {
        Path pathToTrace = getTracePath(path);
        FileOutputStream fos;
        OutputStreamWriter osw;
        try {
            fos = new FileOutputStream(pathToTrace.toString());
            osw = new OutputStreamWriter(fos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        logs.sort(String::compareTo);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append(LS)
                .append("<log xmlns=\"http://www.xes-standard.org/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://www.xes-standard.org/ XES.xsd\" version=\"1.0\">").append(LS);
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
            while (matcher.find()) {
                eventTimestamp += Double.parseDouble(matcher.group(1) == null ? "0" : matcher.group(1));
                Event event = new Event(matcher, eventTimestamp);
                events.add(event);
            }
            start += Math.round(eventTimestamp * 1000);
            sb.append(TAB).append("<trace>").append(LS)
                    .append(TAB).append(TAB).append("<string key=\"concept:name\" value=\"Execution ")
                    .append(getExecutionNumber(log)).append("\"/>").append(LS).append(LS);
            for (Event event : events) {
                sb.append(TAB).append(TAB).append("<event>").append(LS);
                sb.append(event.getEvent());
                sb.append(TAB).append(TAB).append("</event>").append(LS).append(LS);
                if (sb.length() > 2097152) {
                    try {
                        osw.append(sb.toString());
                        sb.delete(0, sb.length());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            sb.append(TAB).append("</trace>").append(LS);
        }
        sb.append("</log>").append(LS);
        try {
            osw.append(sb.toString());
            osw.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starting from the parent folder of the logs, generates the path to the folder (if it does not already exist),
     * creates the traces.xes file (if it already exists it gets deleted) and returns the path to it
     * @param path path to the parent directory of the logs
     * @return the path to the newly created traces.xes file
     */
    private static Path getTracePath(String path) {
        path = path.replace("logs", "traces");
        Path pathToTrace;
        try {
            Path pathToFolder = Paths.get(path);
            Path pathToFile = pathToFolder.resolve("trace.xes");
            if (!Files.exists(pathToFolder)) {
                Files.createDirectories(pathToFolder);
            }
            if (Files.exists(pathToFile)) {
                Files.delete(pathToFile);
            }
            Files.createFile(pathToFile);
            pathToTrace = pathToFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pathToTrace;
    }

    /**
     * Parses the name of the log to return the number of execution
     * @param path path to the log
     * @return number of execution based on the number of the log
     */
    private static int getExecutionNumber(String path) {
        Pattern pattern = Pattern.compile("log([0-9]+)\\.txt");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * This class contains the information of a system call contained in a log file, and is used to rewrite it in the
     * format of an event
     */
    private static class Event {
        String type;
        String call;
        String error;
        String arguments;
        String ret;
        long timestamp;

        public Event(Matcher matcher, double timeFromStart) {
            timestamp = start + Math.round(timeFromStart * 1000);
            if (matcher.group(2) != null) {
                type = "call";
                call = sanitize(matcher.group(3));
                arguments = matcher.group(4) == null ? "" : sanitize(matcher.group(4));
                ret = sanitize(matcher.group(5));
            } else if (matcher.group(7) != null) {
                type = "error";
                error = sanitize(matcher.group(8));
                arguments = sanitize(matcher.group(9));
            } else {
                throw new RuntimeException("Unknown event: " + matcher.group());
            }
        }

        public String getEvent() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = new Date(timestamp);
            String timeString = dateFormat.format(date).replace(" ", "T") + "+02:00";
            if (type.equals("call")) {
                return "" + TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + call + "\"/>" + LS +
                        TAB + TAB + TAB + "<string key=\"arguments\" value=\"" + arguments + "\"/>" + LS +
                        TAB + TAB + TAB + "<date key=\"time:timestamp\" value=\"" + timeString + "\"/>" + LS +
                        TAB + TAB + TAB + "<string key=\"return\" value=\"" + ret + "\"/>" + LS;
            } else {
                return "" + TAB + TAB + TAB + "<string key=\"concept:name\" value=\"" + error + "\"/>" + LS +
                        TAB + TAB + TAB + "<string key=\"arguments\" value=\"" + arguments + "\"/>" + LS;
            }
        }

        /**
         * Method to sanitize the string escaping the reserved characters of xml
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