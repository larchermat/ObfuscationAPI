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

public class CallIndirection implements Transformation {
    private final String path;
    private final ArrayList<String> dirsToExclude;
    private final int limit;

    public CallIndirection(String path, int limit) {
        this.path = path;
        this.limit = limit;
        dirsToExclude = new ArrayList<>();
        dirsToExclude.add("android");
    }

    public CallIndirection(String path, int limit, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.limit = limit;
        this.dirsToExclude = dirsToExclude;
    }

    @Override
    public void obfuscate() throws IOException {
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

            StringBuffer newFile = new StringBuffer();
            StringBuffer temp = new StringBuffer();

            Pattern pattern = Pattern.compile("\\.class (.*) (L.*;)");
            Matcher matcher = pattern.matcher(fileCopy.toString());

            // We want to know if the class is public, because if it isn't we can't keep the new methods introduced to
            // reference them in other classes
            boolean isPublic;
            if (matcher.find()) {
                currentClass = matcher.group(2);
                isPublic = matcher.group(1).contains("public");
            } else {
                break;
            }
            ArrayList<String> methodsAdded = new ArrayList<>();

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
                if (indirectMethods.containsKey(invocation)) {
                    // System.out.println("Duplicate invocation: " + invocation + " in class " + currentClass);
                    method = indirectMethods.get(invocation);
                    newMethod = false;
                } else {
                    method = currentClass + "->method" + numberMethod + "(" + (invocationType.equals("virtual") ? methodClass : "") + methodParameters + ")" + methodReturnType;
                    methodsAdded.add(invocation);
                    indirectMethods.put(invocation, method);
                }
                String replacement = "invoke-static " + methodRegisters + ", " + method;

                matcher.appendReplacement(newFile, replacement.replace("$", "\\$"));

                if (!newMethod) {
                    continue;
                }

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

            if (!isPublic) {
                // If the class isn't public we remove the invocations from the hash map, but only after having edited
                // the current file so that we can reuse them inside the class
                methodsAdded.forEach(indirectMethods::remove);
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
