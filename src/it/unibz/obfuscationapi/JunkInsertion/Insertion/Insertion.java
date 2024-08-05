package it.unibz.obfuscationapi.JunkInsertion.Insertion;

import it.unibz.obfuscationapi.Transformation.Transformation;
import it.unibz.obfuscationapi.Utility.Utilities;

import java.io.*;
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
    final static String junkInstrFileName = "src/it/unibz/obfuscationapi/JunkInsertion/Insertion/junk_instr.txt";

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
        File junkInstrFile = new File(junkInstrFileName);
        Scanner scanner = new Scanner(junkInstrFile);
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
                    // at least 3 const, one for each allocated register
                    replacementContentFirst.add(oneRegConst(junkInstr.getFirst(), newRegs.get(0)));
                    replacementContentFirst.add(oneRegConst(junkInstr.getFirst(), newRegs.get(1)));
                    replacementContentFirst.add(oneRegConst(junkInstr.getFirst(), newRegs.get(2)));

                    // a register can be initialized multiple times
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentFirst.add(oneRegConst(junkInstr.getFirst(), newRegs.get(randInt(0, newRegs.size() - 1))));
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentFirst.add(twoReg(junkInstr.get(randInt(1, 6)), newRegs.get(randInt(1, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));

                    StringBuilder replacement = new StringBuilder(regDirective);
                    for (String s : replacementContentFirst)
                        replacement.append(s);

                    canAddGarbage = true;
                    matcher.appendReplacement(nFile, replacement.toString());
                }
            } else if (matcher.group(3) != null) // invoke-
            {
                if (canAddGarbage) // add garbage to the first invoke- after locals allocation
                {
                    ArrayList<String> replacementContentSecond = new ArrayList<>();

                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentSecond.add(twoReg(junkInstr.get(randInt(1, 6)), newRegs.get(randInt(0, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));
                    for (int i = 0; i < randInt(1, 10); i++)
                        replacementContentSecond.add(twoRegJump(junkInstr.get(randInt(7, junkInstr.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1)), newRegs.get(randInt(0, newRegs.size() - 1))));

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

    private String oneRegConst(String ins, String reg) {
        Scanner sc = new Scanner(ins);
        sc.useDelimiter("VV");
        String s = sc.next() + reg + sc.next();

        Scanner sc1 = new Scanner(s);
        sc1.useDelimiter("LL");
        String toRet = sc1.next() + "0x0";

        sc.close();
        sc1.close();

        return toRet + LS;
    }

    private String twoReg(String ins, String reg1, String reg2) {
        Scanner sc = new Scanner(ins);
        sc.useDelimiter("VV");
        String s = sc.next() + reg1 + sc.next() + reg2;

        sc.close();

        return s + LS;
    }

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