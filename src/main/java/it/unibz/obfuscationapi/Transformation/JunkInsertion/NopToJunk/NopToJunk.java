package it.unibz.obfuscationapi.Transformation.JunkInsertion.NopToJunk;

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
 * Class that applies the nop to junk instruction transformation
 */
public class NopToJunk implements Transformation {
    final String COMPOUND_DELIM = "*";
    final ArrayList<String> dirsToExclude;
    final String path;
    final String junkInstrFileName = Paths.get("it", "unibz", "obfuscationapi", "JunkInsertion", "NopToJunk", "junk_instr.txt").toString();
    final String TO_SUBSTITUTE = "nop" + LS;

    private static ArrayList<ArrayList<String>> junkInstr;

    public NopToJunk(String path) {
        dirsToExclude = new ArrayList<>(List.of("android"));
        this.path = path;
        try {
            junkInstr = loadJunkInstr();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public NopToJunk(String path, ArrayList<String> dirsToExclude) {
        this.dirsToExclude = dirsToExclude;
        this.dirsToExclude.add("android");
        this.path = path;
        try {
            junkInstr = loadJunkInstr();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads all junk instructions from the junk_instr.txt file
     *
     * @return the arraylist containing all junk instructions
     */
    private ArrayList<ArrayList<String>> loadJunkInstr() throws FileNotFoundException {
        ArrayList<ArrayList<String>> als = new ArrayList<>();
        InputStream is = NopToJunk.class.getResourceAsStream("/" + junkInstrFileName);
        InputStreamReader isr;
        try {
            assert is != null;
            isr = new InputStreamReader(is, CHAR_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        Scanner scanner = new Scanner(isr);
        ArrayList<String> instr = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase(COMPOUND_DELIM)) {
                if (!instr.isEmpty()) {
                    als.add(instr);
                }
                instr = new ArrayList<>();
            } else {
                instr.add(line);
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

    @Override
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

    private void process(String path) throws IOException {
        // StringBuffer from file
        StringBuffer copiaFile = getStringBufferFromFile(path);

        // substitute
        StringBuffer nsb = nopToGarbage(copiaFile);

        //rewrite file
        FileOutputStream fos = new FileOutputStream(path);
        OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(nsb.toString());
        out.close();
    }

    private StringBuffer nopToGarbage(StringBuffer sb) {
        Pattern pattern = Pattern.compile(TO_SUBSTITUTE);
        Matcher matcher = pattern.matcher(sb.toString());

        StringBuffer nFile = new StringBuffer();
        int counter = 0;

        while (matcher.find()) {
            int rindex = randInt(0, junkInstr.size() - 1);
            ArrayList<String> al = junkInstr.get(rindex);

            if (rindex == 1) {
                counter++;
                matcher.appendReplacement(nFile, TAB + al.get(0) + "_" + counter + LS +
                        TAB + al.get(1) + "_" + counter + LS);
            } else {
                StringBuilder instr = new StringBuilder();
                for (String s : al)
                    instr.append(TAB).append(s).append(LS);

                matcher.appendReplacement(nFile, instr.toString());
            }

        }

        matcher.appendTail(nFile);

        return nFile;
    }
}
