package it.unibz.obfuscationapi.AdvancedReflection;

import it.unibz.obfuscationapi.Obfuscation.CommandExecution;
import it.unibz.obfuscationapi.Transformation.Transformation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the advanced reflection transformation
 */
public class AdvancedReflection implements Transformation {
    private final String path;
    private final ArrayList<String> dirsToExclude;
    private int instrLength = 0;
    private final String pathToDangerousApi = Paths.get("it", "unibz", "obfuscationapi", "dangerous_api.txt").toString();
    private final String reflectionClass = Paths.get("it", "unibz", "obfuscationapi", "AdvancedReflection", "AdvancedApiReflectionCode.txt").toString();

    private final static Map<String, String> types = Map.of(
            "I", "Ljava/lang/Integer;",
            "Z", "Ljava/lang/Boolean;",
            "B", "Ljava/lang/Byte;",
            "S", "Ljava/lang/Short;",
            "J", "Ljava/lang/Long;",
            "F", "Ljava/lang/Float;",
            "D", "Ljava/lang/Double;",
            "C", "Ljava/lang/Character;"
    );

    private final static Map<String, String> sget = Map.of(
            "I", "Ljava/lang/Integer;->TYPE:Ljava/lang/Class;",
            "Z", "Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;",
            "B", "Ljava/lang/Byte;->TYPE:Ljava/lang/Class;",
            "S", "Ljava/lang/Short;->TYPE:Ljava/lang/Class;",
            "J", "Ljava/lang/Long;->TYPE:Ljava/lang/Class;",
            "F", "Ljava/lang/Float;->TYPE:Ljava/lang/Class;",
            "D", "Ljava/lang/Double;->TYPE:Ljava/lang/Class;",
            "C", "Ljava/lang/Character;->TYPE:Ljava/lang/Class;"
    );

    private final static Map<String, String> cast = Map.of(
            "I", "Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
            "Z", "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;",
            "B", "Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;",
            "S", "Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;",
            "J", "Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;",
            "F", "Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;",
            "D", "Ljava/lang/Double;->valueOf(D)Ljava/lang/Double;",
            "C", "Ljava/lang/Character;->valueOf(C)Ljava/lang/Character;"
    );

    private final static Map<String, String> reverseCast = Map.of(
            "I", "Ljava/lang/Integer;->intValue()I",
            "Z", "Ljava/lang/Boolean;->booleanValue()Z",
            "B", "Ljava/lang/Byte;->byteValue()B",
            "S", "Ljava/lang/Short;->shortValue()S",
            "J", "Ljava/lang/Long;->longValue()J",
            "F", "Ljava/lang/Float;->floatValue()F",
            "D", "Ljava/lang/Double;->doubleValue()D",
            "C", "Ljava/lang/Character;->charValue()C"
    );

    public AdvancedReflection(String path) {
        this.path = path;
        this.dirsToExclude = new ArrayList<>();
        dirsToExclude.add("android");
        dirsToExclude.add("androidx");
        dirsToExclude.add("kotlin");
        dirsToExclude.add("google");
    }

    public AdvancedReflection(String path, ArrayList<String> dirsToExclude) {
        this.path = path;
        this.dirsToExclude = dirsToExclude;
    }

    /**
     * Applies the AdvancedReflection transformation.
     * The method finds amongst all files under the path directory the calls made to methods classified as dangerous api
     * calls. It then substitutes these calls using reflection, inserting the instructions needed to perform the call
     * indirectly in a file called AdvancedApiReflection.smali
     *
     * @throws Exception
     */
    @Override
    public void obfuscate() throws Exception {
        int methodNum = 0;
        StringBuilder smaliReflectionClassCode = new StringBuilder();
        ArrayList<String> files = navigateDirectoryContents(path, dirsToExclude);
        ArrayList<String> dangerousApi = new ArrayList<>(Arrays.asList(getStringBufferFromFile(pathToDangerousApi).toString().split(LS)));
        for (String file : files) {
            // We cannot overrun the limit of instruction inserted in the AdvancedApiReflection.smali file
            int instrLimit = 60000;
            if (instrLength >= instrLimit)
                break;
            StringBuffer fileCopy = getStringBufferFromFile(file);
            StringBuilder newFile = new StringBuilder();
            StringBuilder newMethodBody;

            Pattern pattern = Pattern.compile("(\\.method .*" + LS + ")(?s)(.*?)(\\.end method)");
            Matcher matcher = pattern.matcher(fileCopy.toString());

            if (!matcher.find())
                continue;

            while (matcher.find()) {
                newMethodBody = new StringBuilder();
                String methodBody = matcher.group(2);
                Pattern pattern1 = Pattern.compile("\\.locals ([0-9]+)");
                Matcher matcher1 = pattern1.matcher(methodBody);
                int locals = Integer.parseInt(matcher1.find() ? matcher1.group(1) : "16");
                pattern1 = Pattern.compile("invoke-(virtual|static) \\{(.*)}, ((.*;)->(.*)\\((.*)\\)(V)?(.*)?)(\\s+move-result(?:.*)? ([vp0-9]+))?");
                matcher1 = pattern1.matcher(methodBody);
                while (matcher1.find()) {
                    String methodParameters = matcher1.group(6);
                    ArrayList<String> parameters = splitParameters(methodParameters);
                    if ((locals + calculateRegisters(parameters)) > 11 || !dangerousApi.contains(matcher1.group(3)))
                        continue;

                    String newMoveResult = "";
                    if (matcher1.group(7) == null && matcher1.group(9) != null)
                        newMoveResult = getNewMoveResult(matcher1);
                    String smaliCode = createReflectionMethod(methodNum, locals, matcher1.group(1).equals("virtual"), matcher1.group(2), parameters);
                    smaliCode += newMoveResult;
                    matcher1.appendReplacement(newMethodBody, smaliCode.replace("$", "\\$"));
                    locals += 4;
                    methodNum++;
                    smaliReflectionClassCode.append(addReflectionCode(matcher1.group(4), matcher1.group(5), parameters));
                }
                matcher1.appendTail(newMethodBody);
                String temp = newMethodBody.toString();
                pattern1 = Pattern.compile("(\\.locals )([0-9]+)");
                matcher1 = pattern1.matcher(temp);
                if (matcher1.find()) {
                    temp = matcher1.replaceFirst(matcher1.group(1) + locals);
                }
                String replacement = matcher.group(1) + temp + matcher.group(3);
                matcher.appendReplacement(newFile, replacement.replace("$", "\\$"));
            }
            matcher.appendTail(newFile);

            File f = new File(file);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
            out.append(newFile.toString());
            out.close();
            fos.close();
        }
        String apiReflectionFile = createApiReflectionClassFile();
        StringBuffer apiReflectionCode = getStringBufferFromFile(apiReflectionFile);
        File f = new File(apiReflectionFile);
        FileOutputStream fos = new FileOutputStream(f);
        OutputStreamWriter out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(apiReflectionCode.toString().replace("#!code_to_replace!#", smaliReflectionClassCode));
        out.close();
        fos.close();
    }

    /**
     * Generates the set of instructions for the move result after the new reflection call
     * The call to the obfuscate method of the AdvancedApiReflection class returns the type object, so first the result
     * is stored in the same register, then a check is made to see if it can be cast to the right object, then if the
     * original value was a primitive, a call is made to return the primitive value of the wrapper object
     *
     * @param matcher the matcher matching the invocation and the move result instruction
     * @return the string containing the new move result instruction
     */
    private static String getNewMoveResult(Matcher matcher) {
        String newMoveResult = "";
        if (matcher.group(8).startsWith("[") || matcher.group(8).startsWith("L")) {
            newMoveResult +=
                    TAB + "move-result-object " + matcher.group(10) + LS + LS +
                            TAB + "check-cast " + matcher.group(10) + ", " + matcher.group(8) + LS + LS;
        } else {
            newMoveResult +=
                    TAB + "move-result-object " + matcher.group(10) + LS + LS +
                            TAB + "check-cast " + matcher.group(10) + ", " + types.get(matcher.group(8)) + LS + LS +
                            TAB + "invoke-virtual {" + matcher.group(10) + "}, " + reverseCast.get(matcher.group(8)) + LS + LS +
                            TAB + matcher.group(9);
        }
        return newMoveResult;
    }

    /**
     * Creates the reflection method part of the methodBody
     * The smali code generated declares an array that will contain all registers that represent the parameters needed
     * for the call (except, in case of an invoke-virtual call, the register containing the object whose method we are
     * calling), then a register will be used to store the number of the method we want to invoke (see
     * {@link AdvancedReflection#addReflectionCode(String, String, ArrayList) addReflectionCode} for clarification on
     * the index) and a call will be made to the obfuscate method of the AdvancedApiReflection class passing the number
     * of the method, the register containing the object whose method we are invoking (in case the method is virtual,
     * otherwise we initialize a register to 0 and add it instead) and the array containing the registers needed as
     * parameters
     *
     * @param methodNum   number of the current method
     * @param locals      number of local registers of the current method
     * @param virtual     true if the method to invoke is virtual, false otherwise
     * @param registerStr string containing all registers passed as parameters
     * @param parameters  list containing all parameters split
     * @return the string containing the code to perform the aforementioned instructions
     */
    private String createReflectionMethod(int methodNum, int locals, boolean virtual, String registerStr, ArrayList<String> parameters) {
        String[] registers = registerStr.split(",");
        for (int i = 0; i < registers.length; i++) {
            registers[i] = registers[i].strip();
        }
        LinkedHashMap<String, String> registersByParameter = new LinkedHashMap<>();
        int registerIndex = 0;
        if (virtual) {
            registerIndex++;
        }
        for (String parameter : parameters) {
            String register;
            if (parameter.startsWith("J") || parameter.startsWith("D")) {
                register = String.join(", ", registers[registerIndex], registers[registerIndex + 1]);
                registerIndex += 2;
            } else {
                register = registers[registerIndex];
                registerIndex++;
            }
            registersByParameter.put(parameter, register);
        }
        StringBuilder smaliCode = new StringBuilder("const/4 #reg1#, " + String.format("0x%01X", parameters.size()) + LS + LS);

        if (!parameters.isEmpty()) {
            smaliCode.append(TAB + "new-array #reg1#, #reg1#, [Ljava/lang/Object;").append(LS).append(LS);
            int index = 0;
            for (Map.Entry<String, String> entry : registersByParameter.entrySet()) {
                String castPrimToClass = cast.get(entry.getKey());
                if (castPrimToClass != null) {
                    smaliCode.append(TAB + "invoke-static {").append(entry.getValue()).append("}, ")
                            .append(castPrimToClass).append(LS).append(LS)
                            .append(TAB).append("move-result-object #reg2#").append(LS).append(LS)
                            .append(TAB).append("const/4 #reg4#, ").append(String.format("0x%01X", index)).append(LS).append(LS)
                            .append(TAB).append("aput-object #reg2#, #reg1#, #reg4#").append(LS).append(LS);
                } else {
                    smaliCode.append(TAB + "const/4 #reg3#, ").append(String.format("0x%01X", index)).append(LS).append(LS)
                            .append(TAB).append("aput-object ").append(entry.getValue()).append(", #reg1#, #reg3#").append(LS).append(LS);
                }
                index++;
            }
        }
        smaliCode.append(TAB + "const/16 #reg3#, ").append(String.format("0x%01X", methodNum)).append(LS).append(LS);

        if (virtual) {
            smaliCode.append(TAB + "invoke-static {#reg3#, ")
                    .append(registers[0]).append(", #reg1#}, ")
                    .append("Lcom/apireflectionmanager/AdvancedApiReflection;->obfuscate(")
                    .append("ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
            smaliCode.append(TAB + "const/4 #reg4#, 0x0").append(LS).append(LS)
                    .append(TAB).append("invoke-static {#reg3#, #reg4#, #reg1#}, ")
                    .append("Lcom/apireflectionmanager/AdvancedApiReflection;->obfuscate(")
                    .append("ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        }
        for (int i = 0; i < 4; i++) {
            smaliCode = new StringBuilder(smaliCode.toString().replace("#reg" + (i + 1) + "#", "v" + (locals + i)));
        }
        return smaliCode.toString();
    }

    /**
     * Splits the parameters string generating an arraylist with each element being a parameter
     *
     * @param parameters string containing the parameters
     * @return the arraylist containing the split parameters
     */
    private ArrayList<String> splitParameters(String parameters) {
        ArrayList<String> result = new ArrayList<>();
        String[] parametersArray = parameters.split(";");
        for (String parameter : parametersArray) {
            if (!Objects.equals(parameter, "")) {
                if (parameter.startsWith("L")) {
                    result.add(parameter);
                } else if (parameter.startsWith("[")) {
                    for (int i = 1; i < parameter.length(); i++) {
                        if (parameter.charAt(i) == 'L') {
                            result.add(parameter);
                            break;
                        } else if (parameter.charAt(i) != '[') {
                            result.add(String.valueOf(parameter.charAt(i)));
                            result.addAll(splitParameters(parameter.substring(i + 1)));
                            break;
                        }
                    }
                } else {
                    result.add(String.valueOf(parameter.charAt(0)));
                    result.addAll(splitParameters(parameter.substring(1)));
                }
            }
        }
        return result;
    }

    /**
     * Calculates the number of registers needed for the parameters
     * All primitive types and objects require one register except for the long (J) and double (D) primitive types
     *
     * @param parameters list containing the parameters split
     * @return the number of registers needed to allocate all parameters
     */
    private int calculateRegisters(ArrayList<String> parameters) {
        int count = 0;
        for (String parameter : parameters) {
            if (parameter.startsWith("J") || parameter.startsWith("D"))
                count += 2;
            else
                count++;
        }
        return count;
    }

    /**
     * Generates code that will be inserted in the AdvancedApiReflection.smali file in the constructor.
     * This code creates the instructions to:
     * <ul>
     *     <li>declare an array that will contain the classes of the parameters to perform the original invocation </li>
     *     <li>save the name of the method as a string </li>
     *     <li>save the class we are invoking the method of </li>
     *     <li>instantiate a {@link java.lang.reflect.Method} object containing all the information needed to perform the call</li>
     *     <li>store this object in a static field of this file</li>
     * </ul>
     * The call to the method via reflection will be done by calling the obfuscate method of the AdvancedApiReflection
     * class passing the parameters needed to perform the call and the number of the method that will be used as index
     * to retrieve the {@link java.lang.reflect.Method} object from the static list
     *
     * @param className  name of the class of which we are invoking the method
     * @param methodName name of the method we want to invoke
     * @param params     parameters to be passed when invoking the method
     * @return the string containing the code to insert in the AdvancedApiReflection.smali file to add the current
     * method to the ones invocable via reflection
     */
    private String addReflectionCode(String className, String methodName, ArrayList<String> params) {
        StringBuilder smaliCode = new StringBuilder();
        smaliCode.append(LS).append(TAB)
                .append("const/4 v1, ").append(String.format("0x%01X", params.size())).append(LS).append(LS);
        instrLength++;

        if (!params.isEmpty()) {
            smaliCode.append(TAB).append("new-array v1, v1, [Ljava/lang/Class;").append(LS).append(LS);
            instrLength += 2;
        }

        for (int i = 0; i < params.size(); i++) {
            smaliCode.append(TAB).append("const/4 v2, ").append(String.format("0x%01X", i)).append(LS).append(LS);
            instrLength++;

            String classParam = sget.get(params.get(i));
            if (classParam != null) {
                smaliCode.append(TAB).append("sget-object v3, ").append(classParam).append(LS).append(LS);
                instrLength += 2;
            } else {
                smaliCode.append(TAB).append("const-class v3, ").append(params.get(i)).append(";").append(LS).append(LS);
                instrLength += 2;
            }

            smaliCode.append(TAB).append("aput-object v3, v1, v2").append(LS).append(LS);
            instrLength += 2;
        }

        smaliCode.append(TAB).append("const-class v2, ").append(className).append(LS).append(LS)
                .append(TAB).append("const-string v3, \"").append(methodName).append("\"").append(LS).append(LS);
        instrLength += 4;

        smaliCode.append(TAB).append("invoke-virtual {v2, v3, v1}, Ljava/lang/Class;->getDeclaredMethod(" +
                "Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;").append(LS).append(LS);
        instrLength += 3;

        smaliCode.append(TAB).append("move-result-object v1").append(LS).append(LS)
                .append(TAB).append("sget-object v2, Lcom/apireflectionmanager/AdvancedApiReflection;->")
                .append("obfuscatedMethods:Ljava/util/List;").append(LS).append(LS);
        instrLength += 3;

        smaliCode.append(TAB).append("invoke-interface {v2, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z").append(LS);
        instrLength += 3;
        return smaliCode.toString();
    }

    /**
     * Creates the folder and the file to declare the AdvancedApiReflection class containing the methods to operate the
     * invocation via reflection
     *
     * @return the string path to the file in the working directory
     */
    private String createApiReflectionClassFile() {
        String separator = SEPARATOR;
        if (CommandExecution.os.contains("win")) {
            separator += SEPARATOR;
        }
        Pattern pattern = Pattern.compile(separator + "smali");
        Matcher matcher = pattern.matcher(path);
        Path rflPath = null;
        try {
            if (matcher.find()) {
                Path smaliPath = Paths.get(path.substring(0, matcher.end()));
                Path comDir = smaliPath.resolve("com");
                if (!Files.exists(comDir)) {
                    Path rflPkg = Files.createDirectories(comDir.resolve("apireflectionmanager"));
                    rflPath = rflPkg.resolve("AdvancedApiReflection.smali");
                    createReflectionFile(rflPath.toString());
                } else {
                    Path rflPkg = comDir.resolve("apireflectionmanager");
                    if (!Files.exists(rflPkg)) {
                        Files.createDirectory(rflPkg);
                        rflPath = rflPkg.resolve("AdvancedApiReflection.smali");
                        createReflectionFile(rflPath.toString());
                    } else {
                        rflPath = rflPkg.resolve("AdvancedApiReflection.smali");
                        if (!Files.exists(rflPath)) {
                            createReflectionFile(rflPath.toString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        assert rflPath != null;
        return rflPath.toString();
    }

    /**
     * Creates the AdvancedApiReflection.smali file copying the contents from the txt source file
     * AdvancedApiReflectionCode.txt in the resources folder
     *
     * @param pathToRfl path to the AdvancedApiReflection.smali file
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private void createReflectionFile(String pathToRfl) throws FileNotFoundException, UnsupportedEncodingException {
        StringBuffer text = getStringBufferFromFile(reflectionClass);
        File decryption = new File(pathToRfl);
        PrintStream ps = new PrintStream(new FileOutputStream(decryption));
        ps.print(text);
        ps.close();
    }

}
