package it.unibz.obfuscationapi.Utility;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Utilities {
    public final static String CHAR_ENCODING = "UTF-8";
    public final static char TAB = '\t';
    public final static String LS = System.lineSeparator();
    public final static String SEPARATOR = File.separator;
    private final static Random RANDOM = new Random();

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
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!(dirsToExclude == null) && !dirsToExclude.isEmpty() && !dirsToExclude.contains(file.getName())) {
                    navigateDirectoryContents(file, dirsToExclude, contents);
                }
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
        FileInputStream fis = new FileInputStream(pathName);
        InputStreamReader f = new InputStreamReader(fis, CHAR_ENCODING);

        StringBuffer copy = new StringBuffer();
        Scanner scanner = new Scanner(f);

        while (scanner.hasNextLine())
            copy.append(scanner.nextLine()).append(LS);

        scanner.close();

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

    public static ArrayList<File> searchFiles(File pathFile, String extension, ArrayList<String> dirsToInclude, ArrayList<String> dirsToExclude) {
        return searchFiles(pathFile, extension, new ArrayList<>(), dirsToInclude, dirsToExclude);
    }

    public static ArrayList<File> searchFiles(File pathFile, String extension, ArrayList<File> fileList, ArrayList<String> dirsToInclude, ArrayList<String> dirsToExclude) {
        File[] listFile = pathFile.listFiles();
        if (listFile != null && extension != null) {
            for (File file : listFile) {
                if (file.isDirectory()) {
                    searchFiles(file, extension, fileList, dirsToInclude, dirsToExclude);
                } else {
                    if (checkPath(dirsToInclude, dirsToExclude, file.getPath())) {
                        if (file.getName().endsWith(extension)) {
                            fileList.add(file);
                        }
                    }
                }
            }
        }
        return fileList;
    }

    public static boolean checkPath(ArrayList<String> toInclude, ArrayList<String> toExclude, String path) {
        boolean containsOneToInclude = toInclude == null || toInclude.stream()
                .anyMatch(path::contains);
        boolean doesNotContainAnyToExclude = toExclude == null || toExclude.stream()
                .noneMatch(path::contains);

        return containsOneToInclude && doesNotContainAnyToExclude;
    }

    /**
     * Shuffles elements in an ArrayList
     * @param array ArrayList that will get reordered
     */
    public static <E> void shuffleArray(ArrayList<E> array) {
        for (int i = array.size() - 1; i > 0; i--)
        {
            int index = RANDOM.nextInt(i + 1);
            E a = array.get(index);
            array.set(index, array.get(i));
            array.set(i,a);
        }
    }
}
