package it.unibz.obfuscationapi.Transformation.CallIndirection;

import it.unibz.obfuscationapi.Transformation.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the call indirection transformation: it scans the files contained in the smali directories of the
 * decompiled APK replacing the invocations to methods with the invocation to a new method, that performs the original
 * invocation instead.
 */
public class CallIndirection implements Transformation {
    private final ArrayList<String> dirsToExclude;
    /*
     * To apply the transformation and not break the APK, the class need to keep track of the limit of methods that can
     * be added to files contained in a smali directory. All the classes in a smali directory will be compiled and
     * indexed in a dex file, which can contain at most 65536 methods, so we save the directory to modify together with
     * the number of methods that can be added in total
     */
    private final HashMap<String, Integer> dirsByLimit;

    public CallIndirection(HashMap<String, Integer> dirsByLimit) {
        this.dirsByLimit = dirsByLimit;
        dirsToExclude = new ArrayList<>();
        dirsToExclude.add("android");
        dirsToExclude.add("androidx");
        dirsToExclude.add("kotlin");
    }

    public CallIndirection(HashMap<String, Integer> dirsByLimit, ArrayList<String> dirsToExclude) {
        this.dirsByLimit = dirsByLimit;
        this.dirsToExclude = dirsToExclude;
    }

    /**
     * Applies the call indirection transformation to all files found under the smali directories avoiding the
     * directories in {@link CallIndirection#dirsToExclude dirsToExclude}
     *
     * @throws IOException
     */
    @Override
    public void obfuscate() throws IOException {
        for (String path : dirsByLimit.keySet()) {
            int limit = dirsByLimit.get(path);
            ArrayList<String> files = navigateDirectoryContents(path, dirsToExclude);
            int methodNumber = 1;
            // We keep a hash map of all methods added paired with the call that they substitute, so that in case we
            // find the same call in another file under the same smali folder we can reference the method already
            // created, except for in some special cases (e.g. the class is not public, invocation passes registers
            // containing private volatile fields)
            HashMap<String, String> indirectMethods = new HashMap<>();
            for (String file : files) {
                if (methodNumber >= limit) {
                    break;
                }

                StringBuffer fileCopy = getStringBufferFromFile(file);

                StringBuilder newFile = new StringBuilder();
                StringBuilder temp = new StringBuilder();

                Pattern pattern = Pattern.compile("\\.class (.*) (L.*;)(?s).*\\.source \"(.*?)\"");
                Matcher matcher = pattern.matcher(fileCopy.toString());

                if (!matcher.find())
                    continue;

                // We set a maximum of methods to be added to a class because me may hit the limit before we modify a
                // reasonable number of classes if we substituted every method we found
                int count = 0;
                ArrayList<String> pVFields = getPrivateVolatileFields(fileCopy.toString());
                // We want to know if the class is public, because if it isn't we can't keep the new methods introduced to
                // reference them in other classes
                boolean isPublic = matcher.group(1).contains("public");
                String currentClass = matcher.group(2);
                // If the class is not public we can't reference the methods we create from other classes, but because
                // some big classes are divided in multiple smali files, we can still invoke a method of a non-public
                // class if the current class has the same source
                String source = matcher.group(3);

                // group(1) is the type of invocation: static for static methods or virtual
                // group(2) contains the registers we're passing as parameters for the call
                // group(3) is the name of the class whose method we're invoking
                // group(4) is the name of the method
                // group(5) contains the parameters of the method we're calling
                // group(6) and group(7) are nullable and indicate return type of the call (if group(5) is not null then the
                // return type is void, else the return type is indicated by group(6))
                pattern = Pattern.compile("invoke-(virtual|static) (\\{.*}), (.*;)->(.*)\\((.*)\\)(V)?(.*)?");
                matcher = pattern.matcher(fileCopy.toString());
                while (matcher.find() && methodNumber < limit && count < 3) {
                    String invocationType = matcher.group(1);
                    String methodRegisters = matcher.group(2);
                    String methodClass = matcher.group(3);
                    String methodName = matcher.group(4);
                    String methodParameters = matcher.group(5);
                    String methodReturnType = matcher.group(6) != null ? matcher.group(6) : matcher.group(7);
                    String invocation = methodClass + "->" + methodName + "(" + methodParameters + ")" + methodReturnType;
                    boolean newMethod = true;
                    String method;
                    if (!pVFields.isEmpty() && checkRegisters(fileCopy.substring(0, matcher.start()), methodRegisters, pVFields))
                        continue;
                    if (indirectMethods.containsKey(invocation) || indirectMethods.containsKey(source + invocation)) {
                        method = indirectMethods.get(invocation);
                        if (method == null)
                            method = indirectMethods.get(source + invocation);
                        newMethod = false;
                    } else {
                        method = currentClass + "->method" + methodNumber + "(" + (invocationType.equals("virtual") ? methodClass : "") + methodParameters + ")" + methodReturnType;
                        // We want to save the invocation including the source only if our class is not public, so only
                        // classes with the same source can then invoke this method
                        indirectMethods.put(isPublic ? invocation : source + invocation, method);
                    }
                    String replacement = "invoke-static " + methodRegisters + ", " + method;

                    matcher.appendReplacement(newFile, Matcher.quoteReplacement(replacement));

                    if (!newMethod)
                        continue;

                    int locals;
                    String returnType;
                    if (methodReturnType.equals("V")) {
                        locals = 0;
                        returnType = "-void" + LS;
                    } else if (methodReturnType.equals("J") || methodReturnType.equals("D")) {
                        locals = 2;
                        returnType = "-wide v0" + LS;
                    } else {
                        locals = 1;
                        if (methodReturnType.startsWith("L") || methodReturnType.startsWith("[")) {
                            returnType = "-object v0" + LS;
                        } else {
                            returnType = " v0" + LS;
                        }
                    }

                    temp.append(".method public static method").append(methodNumber).append("(").append((invocationType.equals("virtual") ? methodClass : "")).append(methodParameters).append(")").append(methodReturnType).append(LS);

                    temp.append(TAB).append(".locals ").append(locals).append(LS).append(LS);
                    temp.append(TAB).append("invoke-").append(invocationType).append(" {");
                    int numParameters = occurrences(methodRegisters);
                    for (int i = 0; i < numParameters; i++) {
                        temp.append("p").append(i).append(i == numParameters - 1 ? "" : ", ");
                    }
                    temp.append("}, ").append(invocation).append(LS).append(LS);
                    if (!methodReturnType.equals("V")) {
                        temp.append(TAB).append("move-result").append(returnType).append(LS);
                    }
                    temp.append(TAB).append("return").append(returnType);
                    temp.append(".end method").append(LS).append(LS);
                    methodNumber++;
                    count++;
                }
                matcher.appendTail(newFile);

                newFile.append(LS).append(temp);

                File f = new File(file);
                FileOutputStream fos = new FileOutputStream(f);
                OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
                out.append(newFile.toString());
                out.close();
            }
            System.out.println("Number of methods added " + methodNumber);
        }
    }

    /**
     * Counts how many registers are used in the invocation
     * @param registers string containing all registers used in the invocation
     * @return the number of registers used in the invocation
     */
    private int occurrences(String registers) {
        Pattern pattern = Pattern.compile("([pv][0-9]+)");
        Matcher matcher = pattern.matcher(registers);
        int i = 0;
        while (matcher.find()) {
            i++;
        }
        return i;
    }

    /**
     * Finds and returns all private volatile fields inside the class
     * We want to keep track of these because if they are saved in a register, then we can't invoke a foreign method
     * because the other class can't access them
     *
     * @param classBody body of the class, whose fields we want to inspect
     * @return an arraylist containing all names of the private volatile fields found, if any
     */
    private ArrayList<String> getPrivateVolatileFields(String classBody) {
        Pattern pattern = Pattern.compile("\\.field (?:private )?(?:volatile)? .*? (.*?):");
        Matcher matcher = pattern.matcher(classBody);
        ArrayList<String> fields = new ArrayList<>();
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        return fields;
    }

    /**
     * Checks that the registers initialized for that method invocation do not contain any of the private volatile
     * fields
     * @param trimmedClassBody body of the class trimmed not to include further invocations that may include a new
     *                         assignment of the registers
     * @param methodRegisters registers passed as arguments in the invocation, that need to be checked
     * @param fields list of private volatile fields of the current class
     * @return true if at least one register contains the value of one of the private volatile fields at the moment of
     * invocation, false otherwise
     */
    private boolean checkRegisters(String trimmedClassBody, String methodRegisters, ArrayList<String> fields) {
        Pattern pattern = Pattern.compile("([pv][0-9]+)");
        Matcher matcher = pattern.matcher(methodRegisters);
        ArrayList<String> registers = new ArrayList<>();
        while (matcher.find()) {
            registers.add(matcher.group(1));
        }
        for (String register : registers) {
            pattern = Pattern.compile(register + ", \"(.*)\"");
            matcher = pattern.matcher(trimmedClassBody);
            String lastOccurrence = "";
            while (matcher.find())
                lastOccurrence = matcher.group(1);
            if (fields.contains(lastOccurrence))
                return true;
        }
        return false;
    }

}
