package it.unibz.obfuscationapi.StringEncryption;

import it.unibz.obfuscationapi.Transformation.Transformation;
import it.unibz.obfuscationapi.Utility.Utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the StringEncryption transformation to the decompiled smali files
 */
public class StringEncryption implements Transformation {
    private final ArrayList<String> dirsToExclude;
    private final String path;
    private final String decryptionSrcFile = Paths.get( "it", "unibz", "obfuscationapi", "StringEncryption", "Decryption.txt").toString();

    /**
     *
     * @param dirsToExclude list of directories to exclude from the transformation
     * @param path path to the package of the APK
     */
    public StringEncryption(String path, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.dirsToExclude = dirsToExclude;
    }

    /**
     * Applies the transformation string encryption to the decompiled APK
     */
    @Override
    public void obfuscate() {
        addDecryptionClass();
        ArrayList<String> files;
        try {
            files = Utilities.navigateDirectoryContents(path, dirsToExclude);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String file : files) {
            try {
                process(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Generates (if it does not exist already) the package com/123456789 and, inside of it, creates the
     * Decryption.smali file containing the code to apply the decryption to the strings
     */
    private void addDecryptionClass() {
        Pattern pattern = Pattern.compile(SEPARATOR + "smali");
        Matcher matcher = pattern.matcher(path);
        try {
            if (matcher.find()) {
                Path smaliPath = Paths.get(path.substring(0, matcher.end()));
                Path comDir = smaliPath.resolve("com");
                if (!Files.exists(comDir)) {
                    Path dcrPkg = Files.createDirectories(comDir.resolve("123456789"));
                    Path dcrPath = dcrPkg.resolve("Decryption.smali");
                    createDecryptionFile(dcrPath.toString());
                } else {
                    Path dcrPkg = comDir.resolve("123456789");
                    if (!Files.exists(dcrPkg)) {
                        Files.createDirectory(dcrPkg);
                        Path dcrPath = dcrPkg.resolve("Decryption.smali");
                        createDecryptionFile(dcrPath.toString());
                    } else {
                        Path dcrPath = dcrPkg.resolve("Decryption.smali");
                        if (!Files.exists(dcrPath)) {
                            createDecryptionFile(dcrPath.toString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the Decryption.smali file
     */
    private void createDecryptionFile(String pathToDcr) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuffer text = getStringBufferFromFile(decryptionSrcFile);
        File decryption = new File(pathToDcr);
        PrintStream ps = new PrintStream(new FileOutputStream(decryption));
        ps.print(text);
        ps.close();
    }

    /**
     * Applies the transformation Data Encoding
     *
     * @param filePath path of the file to modify
     * @throws FileNotFoundException
     */
    private void process(String filePath) throws IOException {
        StringBuffer text = getStringBufferFromFile(filePath);
        Pattern pattern = Pattern.compile("(const-string(/jumbo)? )([a-z][0-9]+)(, )(\".*\")");
        Matcher matcher = pattern.matcher(text.toString());
        StringBuilder nFile = new StringBuilder();
        int times = 0;
        while (matcher.find() && times <= 15) {
            String key = "\"" + applyCaesar(matcher.group(5).substring(1, matcher.group(5).length() - 1), 2) + "\"";
            String insert = "    invoke-static {" + matcher.group(3) +
                    "}, Lcom/123456789/Decrypter;->applyCaesar(Ljava/lang/String;)Ljava/lang/String;" + LS +
                    "    move-result-object " + matcher.group(3);
            String replacement = matcher.group(1) + matcher.group(3) + matcher.group(4) + key + LS + insert;
            matcher.appendReplacement(nFile, replacement.replace("$", "\\$"));
            times++;
        }
        matcher.appendTail(nFile);

        FileOutputStream fos = new FileOutputStream(filePath);
        OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(nFile.toString());
        out.close();
    }

    /**
     * Encodes a string with a Caesar code with a specific shift
     *
     * @param text text to encode
     * @param shift shift applied in the encryption
     * @return
     */
    private String applyCaesar(String text, int shift) {
        char[] chars = text.toCharArray();
        boolean skip = false;
        int sc = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = chars[i];
            if (c == '\\') {
                sc = 0;
                skip = true;
                continue;
            }
            if (c == '\"')
                continue;
            if (c == ' ')
                continue;
            if (c == '\n')
                continue;
            if (c == 'Z')
                continue;
            if (c == '\t')
                continue;
            if (c == '\'')
                continue;
            if (c == 'X')
                continue;


            if (c >= 32 && c <= 127) {
                if (skip && sc < 5) {
                    sc++;
                    continue;
                } else
                    skip = false;

            }
            int x = c - 32;
            x = (x + shift) % 96;
            if (x < 0) //java modulo can lead to negative values!
                x += 96;
            chars[i] = (char) (x + 32);
        }

        return new String(chars);
    }

}
