package it.unibz.obfuscationapi.IdentifierRenaming;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.unibz.obfuscationapi.Utility.Utilities.*;

/**
 * Class that applies the renaming transformation: can rename the package of the application or the name of the classes
 */
public class IdentifierRenaming {

    private final ArrayList<File> fileList = new ArrayList<>();
    private String packageIdentifier;
    private final Map<String, String> classes = new HashMap<>();
    private final String path;
    private final String manifestPath;
    private StringBuffer manifestFile;

    /**
     *
     * @param path path to the project's directory
     * @param manifestPath path to the manifest of the project; can be an absolute path or the relative path from the
     *                     project directory
     */
    public IdentifierRenaming(String path, String manifestPath) {
        this.path = path;
        if (manifestPath.contains(path))
            this.manifestPath = manifestPath;
        else{
            Path basePath = Paths.get(this.path);
            Path fullPath = basePath.resolve(manifestPath).normalize();
            this.manifestPath = fullPath.toString();
        }
        try {
            manifestFile = getStringBufferFromFile(this.manifestPath);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        setPackageIdentifier(manifestFile);
    }

    /**
     * Applies the selected transformation to the project
     * @param operation the operation to perform: "renamePackage", "renameClass" or "all"
     * @throws IOException
     */
    public void applyTransformation(String operation) throws IOException {
        if (!operation.equals("renamePackage") && !operation.equals("renameClass") && !operation.equals("all")) {
            throw new IOException(operation + " is not a supported operation");
        }
        File f = new File(path);
        FileOutputStream fos;
        OutputStreamWriter out;
        StringBuffer fileCopy;

        // Generating random name for package
        String newPkgName = generateRandomString(5, null);

        if (operation.equals("renamePackage") || operation.equals("all")) {
            // Look for xml files in the res folder
            File fxml = new File(path + SEPARATOR + "res");
            fileList.clear();
            addFiles(fxml, ".xml");
            // We change every occurrence of the package with the new package name
            for (File file : fileList) {
                fileCopy = getStringBufferFromFile(file.getPath());
                fileCopy = changeXmlPackageName(fileCopy, packageIdentifier.substring(packageIdentifier.lastIndexOf("/") + 1), newPkgName);
                fos = new FileOutputStream(file.getPath());
                out = new OutputStreamWriter(fos, CHAR_ENCODING);
                out.append(fileCopy);
                out.close();
            }
        }

        fileList.clear();
        addFiles(f, ".smali");
        if (operation.equals("renameClass") || operation.equals("all")) {
            // Generate a new name for every class
            for (File fi : fileList) {
                // We take the name of the class (path from smali folder to the class without the extension) and store
                // it in the hashMap together with the new name generated
                if (fi.getPath().contains(packageIdentifier)) {
                    String nameOfClass;
                    do {
                        nameOfClass = generateRandomString(7, null);
                    } while (classes.containsValue(nameOfClass));
                    String pathFile = fi.getPath().substring(fi.getPath().indexOf("smali" + SEPARATOR) + ("smali" + SEPARATOR).length());
                    classes.put(pathFile.replace(".smali", ""), nameOfClass);
                }
            }
        }

        for (File file : fileList) {
            fileCopy = getStringBufferFromFile(file.getPath());

            if (operation.equals("renameClass") || operation.equals("all")) {
                // We change the occurrences of a class with its new name
                fileCopy = changeFileClassName(fileCopy, file);
            }
            if (operation.equals("renamePackage") || operation.equals("all")) {
                try {
                    // We change the occurrences of the package with the new package name
                    fileCopy = changeFilePackageName(fileCopy, newPkgName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            // Modify the smali file
            fos = new FileOutputStream(file.getPath());
            out = new OutputStreamWriter(fos, CHAR_ENCODING);
            out.append(fileCopy);
            out.close();
        }
        // Lastly we change the manifest file to update the name of the main class and/or the name of the package
        if (operation.equals("renameClass") || operation.equals("all"))
            changeManifestMainClass();

        if (operation.equals("renamePackage") || operation.equals("all"))
            manifestFile = changeXmlPackageName(manifestFile, packageIdentifier.substring(packageIdentifier.lastIndexOf("/") + 1), newPkgName);

        fos = new FileOutputStream(manifestPath);
        out = new OutputStreamWriter(fos, CHAR_ENCODING);
        out.append(manifestFile.toString());
        out.close();

    }

    /**
     * Sets the {@link IdentifierRenaming#packageIdentifier packageIdentifier} using / as a separator
     * @param sb string buffer of the manifest file from which we extract the package name
     */
    private void setPackageIdentifier(StringBuffer sb) {
        Pattern pattern = Pattern.compile("(package=\")([a-zA-Z0-9.]*\\.)([a-zA-Z0-9]*)(\")");
        Matcher matcher = pattern.matcher(sb.toString());
        if (matcher.find())
            packageIdentifier = (matcher.group(2) + matcher.group(3)).replace(".", "/");
    }

    /**
     * Changes the occurrences of the package inside a xml file with the new name package name
     * @param sb string buffer with the content of the xml file to modify
     * @param manifestPackage name of the package
     * @param newPkgName new name of the package
     * @return the modified string buffer
     */
    private StringBuffer changeXmlPackageName(StringBuffer sb, String manifestPackage, String newPkgName) {
        // This pattern captures two groups:
        //  1. A sequence of alphanumeric characters, either ":" or "=", the char " , and then a sequence of domain
        //      names separated with dots ( package="com.example  or  id:"com.example )
        //  2. The name of the package we want to substitute
        // Like this we can then replace the content using the first group plus the new package name
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*[:=]\"[a-zA-Z0-9.]*\\.)(" + manifestPackage + ")");
        Matcher matcher = pattern.matcher(sb.toString());
        StringBuffer nFile = new StringBuffer();

        while (matcher.find()) {
            if ((matcher.group(1) + matcher.group(2)).contains(packageIdentifier.replace("/", ".")))
                matcher.appendReplacement(nFile, matcher.group(1) + newPkgName);
        }
        matcher.appendTail(nFile);

        return nFile;
    }

    /**
     * Changes the name of the package in a file
     * @param sb string buffer with the content of the file we are modifying
     * @param newPkgName new name of the package
     * @return the modified string buffer
     */
    private StringBuffer changeFilePackageName(StringBuffer sb, String newPkgName) {
        String manifestPackage = packageIdentifier.substring(packageIdentifier.lastIndexOf("/") + 1);

        Pattern pattern = Pattern.compile("([a-zA-Z. ={},0-9-/]*/)(" + manifestPackage + ")(/[a-zA-Z0-9$,]*)(([a-zA-Z. ={},;:$0-9->/]*/)(" + manifestPackage + ")(/))?");
        Matcher matcher = pattern.matcher(sb.toString());

        StringBuffer nFile = new StringBuffer();

        while (matcher.find()) {
            if ((matcher.group(1) + matcher.group(2)).contains(packageIdentifier) && (matcher.group(5) != null && (matcher.group(5) + matcher.group(6)).contains(packageIdentifier)))
                matcher.appendReplacement(nFile, matcher.group(1) + newPkgName + matcher.group(3).replace("$", "\\$") + (matcher.group(5).replace("$", "\\$") + newPkgName + matcher.group(7)));
            else if ((matcher.group(1) + matcher.group(2)).contains(packageIdentifier))
                matcher.appendReplacement(nFile, matcher.group(1) + newPkgName + matcher.group(3).replace("$", "\\$") + (matcher.group(5) != null ? matcher.group(5).replace("$", "\\$") + matcher.group(6) + matcher.group(7) : ""));
        }

        matcher.appendTail(nFile);

        return nFile;
    }

    /**
     * Changes the name of the classes in a file according to the entries in the {@link IdentifierRenaming#classes classes}
     * hashMap
     * @param sb string buffer with the content of the file in which we look for the classes to change
     * @param file the file we are operating on
     * @return the modified string buffer
     */
    private StringBuffer changeFileClassName(StringBuffer sb, File file) {

        String pathFile = file.getPath().substring(file.getPath().indexOf("smali" + SEPARATOR) + ("smali" + SEPARATOR).length()).replace(".smali", "");
        Pattern pattern = Pattern.compile("(" + packageIdentifier + "/)(([a-zA-Z0-9]*/)*)([a-zA-Z0-9-$]*)|(.source \")([a-zA-Z0-9-$]*)");
        Matcher matcher = pattern.matcher(sb.toString());

        // First alternative groups 1 to 4
        // group(1) is the identifier plus /
        // group(2) is the sequence of packages between group(1) and the name of the class (group(3) is just the last package)
        // group(4) is the ClassName
        // Second alternative groups 5 and 6
        // group(5) + group(6) results in .source "ClassName

        StringBuffer nFile = new StringBuffer();

        while (matcher.find()) {

            if (matcher.group(1) != null) {
                // First case, we have the package identifier (plus possibly some additional packages), and the ClassName
                String target = matcher.group(1) + (matcher.group(2) != null ? matcher.group(2) : "") + matcher.group(4);
                if (classes.containsKey(target)) {
                    String replacement = matcher.group(1) + (matcher.group(2) == null ? matcher.group(2) : "") + classes.get(target);
                    matcher.appendReplacement(nFile, replacement.replace("$", "\\$"));
                }
            } else if (matcher.group(5) != null && classes.containsKey(pathFile)) {
                // Second case, we have .source "ClassName
                matcher.appendReplacement(nFile, matcher.group(5).replace("$", "\\$") + classes.get(pathFile).replace("$", "\\$"));
            }
        }
        matcher.appendTail(nFile);
        return nFile;
    }


    /**
     * Changes the name of the main class in the manifest
     */
    private void changeManifestMainClass() {
        String identifier = packageIdentifier.replace("/", ".");

        Pattern pattern = Pattern.compile("(android:name=\"(" + identifier + ")?\\.?)([a-zA-Z_]*)(\")");
        StringBuffer nFile = new StringBuffer();
        Matcher matcher = pattern.matcher(manifestFile.toString());
        while (matcher.find()) {
            if (matcher.group(2) != null && classes.containsKey(matcher.group(2) + "." + matcher.group(3)))
                matcher.appendReplacement(nFile, matcher.group(1) + classes.get(matcher.group(2) + "." + matcher.group(3)) + matcher.group(4));
            else if (classes.containsKey(packageIdentifier + "/" + matcher.group(3)))
                matcher.appendReplacement(nFile, matcher.group(1) + classes.get(packageIdentifier + "/" + matcher.group(3)) + matcher.group(4));
        }

        matcher.appendTail(nFile);
        manifestFile = nFile;
    }

    /**
     * Adds all files with a particular extension under a parent directory in the fileList ArrayList only if the path
     * contains either one of the dirsToInclude and does not contain any of the dirsToExclude
     * @param pathFile path of the parent directory
     * @param extension extension to look for
     */
    private void addFiles(File pathFile, String extension) {
        ArrayList<String> dirsToInclude = new ArrayList<>(List.of(SEPARATOR + "smali" + SEPARATOR, SEPARATOR + "res" + SEPARATOR));
        ArrayList<String> dirsToExclude = new ArrayList<>(List.of(SEPARATOR + "support" + SEPARATOR));
        searchFiles(pathFile, extension, fileList, dirsToInclude, dirsToExclude);
    }
}


