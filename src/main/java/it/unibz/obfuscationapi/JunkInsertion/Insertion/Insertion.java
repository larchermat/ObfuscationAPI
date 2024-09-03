package it.unibz.obfuscationapi.JunkInsertion.Insertion;

import it.unibz.obfuscationapi.Transformation.Transformation;
import it.unibz.obfuscationapi.Utility.Utilities;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Applies insertion of junk instructions to smali files
 */
public class Insertion implements Transformation {
    final static String COMPOUND_DELIM = "*";
    private final ArrayList<String> dirsToExclude;
    private final String path;
    final static String junkInstrFileName = Paths.get("it", "unibz", "obfuscationapi", "JunkInsertion", "Insertion", "junk_instr.txt").toString();

    private final ArrayList<String> junkInstr;

    private int tmpCounter = 0; // used to ensure the uniqueness of the jump labels

    public Insertion(String path) {
        this.path = path;
        this.dirsToExclude = new ArrayList<>(List.of("android", "adwo", "google"));
        try {
            junkInstr = loadJunkInstr();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Insertion(String path, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.dirsToExclude = new ArrayList<>(dirsToExclude);
        this.dirsToExclude.addAll(List.of("android", "adwo", "google"));
        try {
            junkInstr = loadJunkInstr();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads junk instructions from file
     *
     * @return ArrayList containing in every element the complete junk instruction
     * @throws FileNotFoundException if the junk instruction file is not found
     */
    private ArrayList<String> loadJunkInstr() throws FileNotFoundException {
        ArrayList<String> als = new ArrayList<>();
        InputStream is = Insertion.class.getResourceAsStream("/" + junkInstrFileName);
        InputStreamReader isr;
        try {
            assert is != null;
            isr = new InputStreamReader(is, CHAR_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Scanner scanner = new Scanner(isr);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.equalsIgnoreCase(COMPOUND_DELIM)) {
                als.add(line + LS);
            } else {
                StringBuilder compInstr = new StringBuilder();
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if (line.equalsIgnoreCase(COMPOUND_DELIM))
                        break;
                    compInstr.append(line).append(LS);
                }
                als.add(compInstr.toString());
            }
        }
        scanner.close();
        try {
            isr.close();
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return als;
    }

    /**
     * Applies the junk code insertion transformation to the decompiled APK
     */
    public void obfuscate() {
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
     * Applies the junk insertion to the specified file
     *
     * @param path path of the file to process
     * @throws IOException if out append() or close() fail or see {@link Utilities#getStringBufferFromFile(String)}
     */
    private void process(String path) throws IOException {
        // StringBuffer from file
        StringBuffer fileCopy = getStringBufferFromFile(path);

        // substitute
        StringBuffer nsb = garbage(fileCopy);

        //rewrite file
        FileOutputStream fos = new FileOutputStream(path);
        OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(nsb.toString());
        out.close();
    }

    /**
     * This method accepts the StringBuffer containing the smali file and reads through it inserting the garbage code
     * <br>
     * Once a match is found for the pattern, if canAddGarbage flag is true, then we can add garbage instructions such
     * as useless conditional jumps. Only after we match the locals pattern, and we insert our new registers, we can
     * ensure that all other junk instructions will work
     *
     * @param sb StringBuffer containing the smali code
     * @return the StringBuffer with the modified code
     */
    private StringBuffer garbage(StringBuffer sb) {
        Pattern pattern = Pattern.compile("(.locals )([0-9]*)|(invoke-)|(.end method)");
        Matcher matcher = pattern.matcher(sb.toString());
        ArrayList<String> newRegs = new ArrayList<>();
        StringBuffer nFile = new StringBuffer();
        boolean canAddGarbage = false;
        while (matcher.find()) {
            if (matcher.group(1) != null) // .locals
            {
                int nLocals = Integer.parseInt(matcher.group(2));

                if (nLocals <= 5) {
                    int newLocals = nLocals + 3;
                    String regDirective = matcher.group(1) + newLocals + LS;

                    newRegs = new ArrayList<>();
                    newRegs.add("v" + (nLocals));
                    newRegs.add("v" + (nLocals + 1));
                    newRegs.add("v" + (nLocals + 2));

                    ArrayList<String> replacementContentFirst = new ArrayList<>();
                    // at least 3 initializations, one for each allocated register
                    replacementContentFirst.add(oneRegConst(newRegs.get(0)));
                    replacementContentFirst.add(oneRegConst(newRegs.get(1)));
                    replacementContentFirst.add(oneRegConst(newRegs.get(2)));

                    // a register can be initialized multiple times
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentFirst.add(oneRegConst(newRegs.get(randInt(0, newRegs.size() - 1))));
                    // adding lines of junk code that use the instantiated registers
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentFirst.add(twoReg(junkInstr.get(randInt(0, 5)), newRegs.get(randInt(1, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));

                    StringBuilder replacement = new StringBuilder(regDirective);
                    for (String s : replacementContentFirst)
                        replacement.append(s);

                    canAddGarbage = true;
                    matcher.appendReplacement(nFile, replacement.toString());
                }
            } else if (matcher.group(3) != null) // invoke-
            {
                if (canAddGarbage) // the registers were allocated in this method, so now we can add the following junk code
                {
                    ArrayList<String> replacementContentSecond = new ArrayList<>();

                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentSecond.add(twoReg(junkInstr.get(randInt(0, 5)), newRegs.get(randInt(0, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentSecond.add(twoRegJump(junkInstr.get(randInt(6, junkInstr.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));

                    StringBuilder replacement = new StringBuilder();
                    for (String s : replacementContentSecond)
                        replacement.append(s);

                    replacement.append(LS);
                    matcher.appendReplacement(nFile, replacement + matcher.group(3));

                    canAddGarbage = false;
                }
            } else // end-method
            {
                canAddGarbage = false;
            }
        }

        matcher.appendTail(nFile);

        return nFile;
    }

    /**
     * Method that given a register name returns a string of the initialization of the register setting it to the value
     * of 0
     *
     * @param reg register to initialize
     * @return the string containing the initialization
     */
    private static String oneRegConst(String reg) {
        return "const/4 " + reg + ",0x0" + LS;
    }

    /**
     * Given an instruction (taken from the first six instructions in the junk_instr.txt file) and two registers, the
     * method inserts the two registers in the instruction and returns it
     *
     * @param ins  instruction
     * @param reg1 first register
     * @param reg2 second register
     * @return the string containing the line of junk code using the two registers
     */
    private String twoReg(String ins, String reg1, String reg2) {
        Scanner sc = new Scanner(ins);
        sc.useDelimiter("VV");
        String s = sc.next() + reg1 + sc.next() + reg2;

        sc.close();

        return s + LS;
    }

    /**
     * Given an instruction (taken from the seventh line onwards of the junk_instr.txt file) and two registers, the
     * method inserts the two registers in the instruction and returns it<br>
     * All instructions returned from this method will have a conditional jump, that jumps just to the next line
     *
     * @param ins  instruction
     * @param reg1 first register
     * @param reg2 second register
     * @return the string containing the instruction with the two registers
     */
    private String twoRegJump(String ins, String reg1, String reg2) {
        Scanner sc = new Scanner(ins);
        sc.useDelimiter("VV");
        String s = sc.next() + reg1 + sc.next() + reg2 + sc.next();

        Scanner sc1 = new Scanner(s);
        sc1.useDelimiter("TT");
        String toRet = sc1.next() + "Target_" + tmpCounter + " " + sc1.next() + "Target_" + tmpCounter;
        tmpCounter++;

        sc.close();
        sc1.close();

        return toRet + LS;
    }

}