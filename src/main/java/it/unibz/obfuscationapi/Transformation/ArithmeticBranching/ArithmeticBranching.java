package it.unibz.obfuscationapi.Transformation.ArithmeticBranching;

import it.unibz.obfuscationapi.Transformation.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the arithmetic branching transformation to smali files.
 * The arithmetic branching creates possible alternative branches (that are not executed) via an arithmetic check,
 * inserting some goto instructions and labels but preserving the code functionality
 */
public class ArithmeticBranching implements Transformation {
    private final String path;
    private final ArrayList<String> dirsToExclude;

    public ArithmeticBranching(String path) {
        this.path = path;
        this.dirsToExclude = new ArrayList<>();
        dirsToExclude.add("android");
        dirsToExclude.add("androidx");
        dirsToExclude.add("kotlin");
        dirsToExclude.add("google");
    }

    public ArithmeticBranching(String path, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.dirsToExclude = dirsToExclude;
    }

    /**
     * Applies the arithmetic branching transformation to all files under path excluding the files under the directories
     * to exclude. This method alters only methods that are neither abstract nor native and only if they have at least
     * two local registers instantiated.
     * The transformation preserves the code functionality but creates possible branches for the execution.
     *
     * <blockquote> <pre>
     *      .locals
     *      instructions
     *      return
     * </pre> </blockquote>
     * <p>
     * Becomes
     *
     * <blockquote> <pre>
     *      .locals
     *      v0 = 1
     *      v1 = 2
     *      v0 = v0 + v1
     *      v0 = v0 - v1
     *      if v0 > 0
     *          goto :temp
     *      goto :end
     *      :temp
     *      :start
     *      instructions
     *      return
     *      :end
     *      goto :start
     * </pre> </blockquote>
     *
     * @throws Exception
     */
    @Override
    public void obfuscate() throws Exception {
        ArrayList<String> files = navigateDirectoryContents(path, dirsToExclude);
        for (String file : files) {
            StringBuffer fileCopy = getStringBufferFromFile(file);
            StringBuilder nFile = new StringBuilder();
            String regex = "(\\.method .*" + LS + ")(?s)(.*?)(\\.end method)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(fileCopy);
            while (matcher.find()) {
                if (matcher.group(1).contains("abstract") || matcher.group(1).contains("native"))
                    continue;

                Pattern pattern1 = Pattern.compile("(\\.locals ([0-9]+))(?s)(.*)");
                Matcher matcher1 = pattern1.matcher(matcher.group(2));
                if (!matcher1.find())
                    continue;
                int locals = Integer.parseInt(matcher1.group(2));
                if (locals < 2)
                    continue;
                String startLabel = generateRandomString(16, null);
                String endLabel = generateRandomString(16, null);
                String tempLabel = generateRandomString(16, null);
                int v0 = randInt(1, 32);
                int v1 = randInt(1, 32);
                String temp = matcher1.group(1) + LS + LS +
                        TAB + "const v0, " + String.format("0x%01X", v0) + LS + LS +
                        TAB + "const v1, " + String.format("0x%01X", v1) + LS + LS +
                        TAB + "add-int v0, v0, v1" + LS + LS +
                        TAB + "rem-int v0, v0, v1" + LS + LS +
                        TAB + "if-gtz v0, :" + tempLabel + LS + LS +
                        TAB + "goto/32 :" + endLabel + LS + LS +
                        TAB + ":" + tempLabel + LS + LS +
                        TAB + ":" + startLabel +
                        matcher1.group(3) + LS +
                        TAB + ":" + endLabel + LS + LS +
                        TAB + "goto/32 :" + startLabel + LS;
                matcher.appendReplacement(nFile, Matcher.quoteReplacement(matcher.group(1) + temp + matcher.group(3)));
            }
            matcher.appendTail(nFile);
            File f = new File(file);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
            out.append(nFile.toString());
            out.close();
            fos.close();
        }
    }
}
