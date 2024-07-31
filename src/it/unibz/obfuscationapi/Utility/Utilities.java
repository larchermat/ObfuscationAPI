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

    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
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
     * @param pathName path of the file
     * @return StringBuffer containing the file contents
     * @throws FileNotFoundException if the file does not exist or isn't found
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
}
