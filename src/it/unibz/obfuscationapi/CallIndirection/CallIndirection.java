package it.unibz.obfuscationapi.CallIndirection;

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

// TODO: try to move a whole class declaration (including the segments) from one dex to the other and add methods from there, avoiding cross dex invocations because it might count those methods towards the limit

public class CallIndirection implements Transformation {
    private final ArrayList<String> dirsToExclude;
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

    @Override
    public void obfuscate() throws IOException {
        for (String path : dirsByLimit.keySet()) {
            int limit = dirsByLimit.get(path);
            ArrayList<String> files = navigateDirectoryContents(path, dirsToExclude);
            int numberMethod = 1;
            HashMap<String, String> indirectMethods = new HashMap<>();
            for (String file : files) {
                int count = 0;
                if (numberMethod >= limit) {
                    break;
                }
                String currentClass;
                int numParameters, i;
                File f = new File(file);
                StringBuffer fileCopy = getStringBufferFromFile(file);

                StringBuilder newFile = new StringBuilder();
                StringBuilder temp = new StringBuilder();

                Pattern pattern = Pattern.compile("\\.class (.*) (L.*;)(?s).*\\.source \"(.*?)\"");
                Matcher matcher = pattern.matcher(fileCopy.toString());

                // We want to know if the class is public, because if it isn't we can't keep the new methods introduced to
                // reference them in other classes
                String source;
                boolean isPublic;
                if (matcher.find()) {
                    currentClass = matcher.group(2);
                    isPublic = matcher.group(1).contains("public");
                    // If the class is not public we can't reference the methods we create from other classes, but because
                    // some big classes are divided in multiple smali files, we can still invoke a method of a non-public
                    // class if the current class has the same source
                    source = matcher.group(3);
                } else {
                    continue;
                }

                // group(1) is the type of invocation: static for static methods or virtual
                // group(2) contains the registers we're passing as parameters for the call
                // group(3) is the name of the class whose method we're invoking
                // group(4) is the name of the method
                // group(5) contains the parameters of the method we're calling
                // group(6) and group(7) are nullable and indicate return type of the call (if group(5) is not null then the
                // return type is void, else the return type is indicated by group(6))
                pattern = Pattern.compile("invoke-(virtual|static) (\\{.*}), (.*;)->(.*)\\((.*)\\)(V)?(.*)?");
                matcher = pattern.matcher(fileCopy.toString());
                while (matcher.find() && numberMethod < limit && count < 3) {
                    String invocationType = matcher.group(1);
                    String methodRegisters = matcher.group(2);
                    String methodClass = matcher.group(3);
                    String methodName = matcher.group(4);
                    String methodParameters = matcher.group(5);
                    String methodReturnType = matcher.group(6) != null ? matcher.group(6) : matcher.group(7);
                    String invocation = methodClass + "->" + methodName + "(" + methodParameters + ")" + methodReturnType;
                    boolean newMethod = true;
                    String method;
                    if (indirectMethods.containsKey(invocation) || indirectMethods.containsKey(source + invocation)) {
                        method = indirectMethods.get(invocation);
                        if (method == null)
                            method = indirectMethods.get(source + invocation);
                        newMethod = false;
                    } else {
                        method = currentClass + "->method" + numberMethod + "(" + (invocationType.equals("virtual") ? methodClass : "") + methodParameters + ")" + methodReturnType;
                        // We want to save the invocation including the source only if our class is not public, so only
                        // classes with the same source can then invoke this method
                        indirectMethods.put(isPublic ? invocation : source + invocation, method);
                    }
                    String replacement = "invoke-static " + methodRegisters + ", " + method;

                    matcher.appendReplacement(newFile, replacement.replace("$", "\\$"));

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

                    temp.append(".method public static method").append(numberMethod).append("(").append((invocationType.equals("virtual") ? methodClass : "")).append(methodParameters).append(")").append(methodReturnType).append(LS);
                    numParameters = occurrences(methodRegisters);

                    temp.append(TAB).append(".locals ").append(locals).append(LS).append(LS);
                    temp.append(TAB).append("invoke-").append(invocationType).append(" {");
                    for (i = 0; i < numParameters; i++) {
                        temp.append("p").append(i).append(i == numParameters - 1 ? "" : ", ");
                    }
                    temp.append("}, ").append(invocation).append(LS).append(LS);
                    if (!methodReturnType.equals("V")) {
                        temp.append(TAB).append("move-result").append(returnType).append(LS);
                    }
                    temp.append(TAB).append("return").append(returnType);
                    temp.append(".end method").append(LS).append(LS);
                    numberMethod++;
                    count++;
                }
                matcher.appendTail(newFile);

                newFile.append(LS).append(temp);

                FileOutputStream fos = new FileOutputStream(f);
                OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
                out.append(newFile.toString());
                out.close();
            }
            System.out.println("Number of methods added " + numberMethod);
        }
    }

    private int occurrences(String s) {
        Pattern pattern = Pattern.compile("([pv][0-9]+)");
        Matcher matcher = pattern.matcher(s);
        int i = 0;
        while (matcher.find()) {
            i++;
        }
        return i;
    }
}
