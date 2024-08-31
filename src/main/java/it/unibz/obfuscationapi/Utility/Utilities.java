package it.unibz.obfuscationapi.Utility;

import it.unibz.obfuscationapi.Obfuscation.CommandExecution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 * Class containing static fields and methods useful in multiple transformations
 */
public class Utilities {
    public final static String CHAR_ENCODING = "UTF-8";
    public final static char TAB = '\t';
    public final static String LS = System.lineSeparator();
    public final static String SEPARATOR = File.separator;
    private final static Random RANDOM = new Random();
    private final static String pathOfProject = Paths.get("").toAbsolutePath().toString();
    private static int errorLogCount = 0;

    public static int randInt(int max) {
        return randInt(0, max);
    }

    public static int randInt(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Find all files contained in a directory navigating all child directories
     *
     * @param path          path of the directory
     * @param dirsToExclude list of directories to exclude
     * @return the list of files found in the directories
     * @throws IOException if file getCanonicalPath fails
     */
    public static ArrayList<String> navigateDirectoryContents(String path, ArrayList<String> dirsToExclude) throws IOException {
        return navigateDirectoryContents(new File(path), dirsToExclude, new ArrayList<>());
    }

    private static ArrayList<String> navigateDirectoryContents(File dir, ArrayList<String> dirsToExclude, ArrayList<String> contents) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println("No files found in directory " + dir.getAbsolutePath());
            return contents;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (checkPath(null, dirsToExclude, file.getPath()))
                    navigateDirectoryContents(file, dirsToExclude, contents);
            } else {
                contents.add(file.getCanonicalPath());
            }
        }
        return contents;
    }

    /**
     * Returns file contents as a string buffer
     *
     * @param pathName path of the file
     * @return StringBuffer containing the file contents
     * @throws FileNotFoundException        if the file does not exist or isn't found
     * @throws UnsupportedEncodingException if the named charset is not supported
     */
    public static StringBuffer getStringBufferFromFile(String pathName) throws FileNotFoundException, UnsupportedEncodingException {
        InputStreamReader f;
        FileInputStream fis = null;
        InputStream is = null;
        if (Files.exists(Paths.get(pathName))) {
            fis = new FileInputStream(pathName);
            f = new InputStreamReader(fis, CHAR_ENCODING);
        } else {
            is = Utilities.class.getResourceAsStream("/" + pathName);
            assert is != null;
            f = new InputStreamReader(is, CHAR_ENCODING);
        }

        StringBuffer copy = new StringBuffer();
        Scanner scanner = new Scanner(f);

        while (scanner.hasNextLine())
            copy.append(scanner.nextLine()).append(LS);

        scanner.close();
        try {
            f.close();
            if (fis != null)
                fis.close();
            if (is != null)
                is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return copy;
    }

    /**
     * Generates a random string of a given length using a given charset, alternatively using lower and upper case chars
     * if no other charset is provided
     *
     * @param stringLen length of the random string
     * @param charset   charset to use to generate the random string, if null or an empty string is given, the default
     *                  charset is used
     * @return the resulting random string
     */
    public static String generateRandomString(int stringLen, String charset) {
        if (charset == null || charset.isEmpty()) {
            charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }
        StringBuilder builder = new StringBuilder(stringLen);
        for (int i = 0; i < stringLen; i++) {
            builder.append(charset.charAt(randInt(charset.length() - 1)));
        }
        return builder.toString();
    }

    /**
     * Finds all files with a certain extension under a parent directory
     * When the lists dirsToInclude and dirsToExclude are null, then no check is performed, otherwise, the path under
     * which the files are searched needs to include at least one of the directories to include, and none of the ones to
     * exclude
     *
     * @param dirPath       path of the parent directory in which the search is performed
     * @param extension     extension of the files to find
     * @param dirsToInclude list of directories of which at least one needs to be included in the path
     * @param dirsToExclude list of directories of which none must be included in the path
     * @return an arraylist containing all the file objects pointing to the files under the directory with the desired
     * extension
     */
    public static ArrayList<File> searchFiles(File dirPath, String extension, ArrayList<String> dirsToInclude, ArrayList<String> dirsToExclude) {
        return searchFiles(dirPath, extension, new ArrayList<>(), dirsToInclude, dirsToExclude);
    }

    public static ArrayList<File> searchFiles(File pathFile, String extension, ArrayList<File> fileList, ArrayList<String> dirsToInclude, ArrayList<String> dirsToExclude) {
        File[] files = pathFile.listFiles();
        if (files != null && extension != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (checkPath(dirsToInclude, dirsToExclude, file.getPath()))
                        searchFiles(file, extension, fileList, dirsToInclude, dirsToExclude);
                } else {
                    if (file.getName().endsWith(extension))
                        fileList.add(file);
                }
            }
        }
        return fileList;
    }

    /**
     * Checks if the current path from the project folder onwards contains at least one (if any) of the directories to
     * include and does not contain any (if any) of the directories to exclude
     *
     * @param toInclude list of directories of which at least one needs to be included in the path
     * @param toExclude list of directories of which none must be included in the path
     * @param path      path to be checked
     * @return true if the path contains at least one directory of those to include and does not contain any of the ones
     * to exclude, false otherwise
     */
    public static boolean checkPath(ArrayList<String> toInclude, ArrayList<String> toExclude, String path) {
        path = path.replace(pathOfProject, "");
        String separator = SEPARATOR;
        if (CommandExecution.os.contains("win")) {
            separator += SEPARATOR;
        }
        ArrayList<String> subDirs = new ArrayList<>(Arrays.stream(path.split(separator))
                .toList());
        boolean containsOneToInclude = toInclude == null || toInclude.isEmpty() || toInclude.stream()
                .anyMatch(subDirs::contains);
        boolean doesNotContainAnyToExclude = toExclude == null || toExclude.isEmpty() || toExclude.stream()
                .noneMatch(subDirs::contains);

        return containsOneToInclude && doesNotContainAnyToExclude;
    }

    /**
     * Shuffles elements in an ArrayList
     *
     * @param array ArrayList that will get reordered
     */
    public static <E> void shuffleArray(ArrayList<E> array) {
        for (int i = array.size() - 1; i > 0; i--) {
            int index = RANDOM.nextInt(i + 1);
            E a = array.get(index);
            array.set(index, array.get(i));
            array.set(i, a);
        }
    }

    /**
     * Resolves a full path for a directory when unsure if it is already a full path or a relative path to a base directory
     *
     * @param base    the path of the parent directory
     * @param dirPath the path of the directory we want the full path of
     * @return string containing the full path leading to the directory
     */
    public static String resolveFullPath(String base, String dirPath) {
        if (dirPath.contains(base))
            return dirPath;
        else {
            Path basePath = Paths.get(base);
            Path fullPath = basePath.resolve(dirPath).normalize();
            return fullPath.toString();
        }
    }

    synchronized public static void writeErrorLog(StringBuilder args) throws IOException {
        Path path = Paths.get("errors", "error" + errorLogCount + ".txt");
        errorLogCount++;
        File errorLog = new File(path.toString());
        if (errorLog.exists()) {
            Files.delete(path);
        }
        Files.createFile(path);
        FileOutputStream fos = new FileOutputStream(errorLog);
        OutputStreamWriter osw = new OutputStreamWriter(fos, CHAR_ENCODING);
        osw.append(args.toString());
        osw.close();
        fos.close();
    }
}
