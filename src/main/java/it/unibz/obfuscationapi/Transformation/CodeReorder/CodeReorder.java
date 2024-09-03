package it.unibz.obfuscationapi.Transformation.CodeReorder;

import it.unibz.obfuscationapi.Transformation.Transformation;
import it.unibz.obfuscationapi.Utility.Utilities;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the code reordering transformation to the smali files
 */
public class CodeReorder implements Transformation {
    private final String path;
    private final ArrayList<String> dirsToExclude;

    /**
     * @param path path to the package folder containing the smali directories
     */
    public CodeReorder(String path) {
        this.path = path;
        this.dirsToExclude = new ArrayList<>();
        dirsToExclude.add("android");
        dirsToExclude.add("androidx");
        dirsToExclude.add("data");
    }

    /**
     * @param path          path to the package folder containing the smali directories
     * @param dirsToExclude directories to exclude from the transformation
     */
    public CodeReorder(String path, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.dirsToExclude = dirsToExclude;
        dirsToExclude.add("android");
        dirsToExclude.add("androidx");
        dirsToExclude.add("data");
    }

    /**
     * Collects the files in the {@link CodeReorder#path path} directory and applies the transformation to each one
     */
    @Override
    public void obfuscate() throws IOException {
        ArrayList<String> files = Utilities.navigateDirectoryContents(path, dirsToExclude);
        for (String file : files) {
            process(file);
        }
    }

    /**
     * Performs the code reorder transformation on a smali file
     *
     * @param filePath path of the file to be modified
     * @throws IOException
     */
    private void process(String filePath) throws IOException {
        StringBuffer text = getStringBufferFromFile(filePath);

        String regex = "(# (?:virtual|direct) methods" + LS + ")?(\\.method .*" + LS + ")(?s)(.*?)(\\.end method)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text.toString());
        StringBuilder nFile = new StringBuilder();
        while (matcher.find()) {

            //smali code directives will be rewritten in the new smali file
            String directive = matcher.group(1) != null ? matcher.group(1) : "";
            //the old method
            String method = matcher.group(3);

            String newMethod = reorderMethod(method);
            String replacement = directive + matcher.group(2) + newMethod + matcher.group(4);
            matcher.appendReplacement(nFile, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(nFile);
        FileOutputStream fos = new FileOutputStream(filePath);
        OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(nFile.toString());
        out.close();
    }

    /**
     * Reorders the instructions inside a given method keeping the logical order and the functionality
     * It will skip empty methods and methods with every kind of jump instructions in order to save the correct logical
     * operation
     *
     * <blockquote> <pre>
     *     instruction_1;
     *     instruction_2;
     *     instruction_3; // The last instruction corresponds to the return statement
     * </pre> </blockquote>
     * <p>
     * Becomes
     *
     * <blockquote> <pre>
     *     :goto i:1
     *     i:2
     *      instruction_2
     *      :goto i:3
     *     i:3
     *      instruction_3
     *     i:1
     *      instruction_1
     *      :goto i:2
     * </pre> </blockquote>
     *
     * @param method string containing the method to which the transformation is applied
     * @return the modified method (or the original if the transformation is not applicable)
     */
    private String reorderMethod(String method) {
        /*
         * in all this cases the method will be rewritten without any modification otherwise the logical operation
         * will be compromised
         */
        if (method.contains(".end sparse-switch") || method.contains(".end packed-switch")
                || method.contains(".end array-data") || method.contains("value = {") ||
                method.contains(".end annotation") ||
                (method.contains("if-eq") || method.contains("if-ne") ||
                        method.contains("if-lt") || method.contains("if-ge") ||
                        method.contains("if-gt") || method.contains("if-le")) || method.contains("goto") || method.contains("try_end")
        ) {
            return method;
        }

        StringTokenizer st = new StringTokenizer(method, LS);

        // Methods with no instructions inside will be returned without any change
        if (st.countTokens() <= 1) {
            return method;
        }

        HashMap<Integer, String> instructionsOrder = new HashMap<>();
        ArrayList<Integer> order = new ArrayList<>();

        int index = 0;
        String local = st.nextToken();
        boolean temp = false;
        String stemp = null;
        while (st.hasMoreTokens()) {
            /*
             * Every instruction goes into the hash map with index as a key.
             * If an invoke instruction is found, it will be paired up with the following instruction move, otherwise an
             * error will occur while recompiling with apktool
             */
            String s;
            if (!temp)
                s = st.nextToken();
            else {
                s = stemp;
                temp = false;
            }

            if (s.contains("invoke")) {
                String s1 = st.nextToken();
                if (s1.contains("move")) {
                    index++;
                    instructionsOrder.put(index, s + LS + s1);
                    order.add(index);
                } else {
                    index++;
                    instructionsOrder.put(index, s);
                    order.add(index);
                    if (!s1.contains("invoke")) {
                        index++;
                        instructionsOrder.put(index, s1);
                        order.add(index);
                    } else {
                        stemp = s1;
                        temp = true;
                    }
                }
            } else {
                index++;
                instructionsOrder.put(index, s);
                order.add(index);
            }
        }
        // We shuffle the instructions' order
        shuffleArray(order);

        StringBuilder newMethod = new StringBuilder();
        newMethod.append(local).append("\n");
        newMethod.append("\n");
        newMethod.append("goto :i_1\n");

        for (int current : order) {
            newMethod.append(":i_").append(current).append("\n");
            newMethod.append(instructionsOrder.get(current));
            newMethod.append("\n");
            if (current < order.size()) {
                int next = current + 1;
                newMethod.append("goto :i_").append(next).append("\n");
            }
        }
        return newMethod.toString();
    }

}
