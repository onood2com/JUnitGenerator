package aka.junitgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Generate Junit test.
 * Quick & dirty ;)
 */
public final class JunitTestGenerator {

    private static @NonNull final Logger LOGGER = Logger.getLogger("aka.junitgenerator.JUnitGenerator.MediaInfoJavaGenerator");

    /**
     * Generate JUnit tests from classes presents in the given jar or classes directory.
     *
     * @param destinationDirectory destination directory for generated classes.
     * @param absolutePath absolute path to the jar/directory classes.
     * @param dependentJarsListAbsolutePath list of mandatory additional jars.
     */
    public void generateJunitTestClasses(@NonNull final String destinationDirectory, @NonNull final String absolutePath, @NonNull final List<String> dependentJarsListAbsolutePath) {
        try {
            Map<String, List<String>> classNameListByPackageNameMap = null;
            if (absolutePath.endsWith(".jar")) {
                classNameListByPackageNameMap = getAllObjectListFromJar(absolutePath);
            } else {
                classNameListByPackageNameMap = getAllObjectListFromDirectory(absolutePath);
            }
            final URL[] urls = new URL[1 + dependentJarsListAbsolutePath.size()];
            urls[0] = new File(absolutePath).toURI().toURL();
            int i = 1;
            for (final String dependentJarsAbsolutePath : dependentJarsListAbsolutePath) {
                urls[i] = new File(dependentJarsAbsolutePath).toURI().toURL();
                i++;
            }
            final URLClassLoader classLoaderForJar = new URLClassLoader(urls);
            for (final Entry<String, List<String>> currentEntryMap : classNameListByPackageNameMap.entrySet()) {
                final String packageName = currentEntryMap.getKey();
                final List<String> classNameList = currentEntryMap.getValue();

                for (final String className : classNameList) {
                    final String classNameResource = packageName + "." + className;
                    try {
                        final Class<?> crunchifyClass = classLoaderForJar.loadClass(classNameResource);
                        if (crunchifyClass != null) {
                            if (!Modifier.isAbstract(crunchifyClass.getModifiers()) && !Modifier.isInterface(crunchifyClass.getModifiers())) {
                                createFile(crunchifyClass, destinationDirectory);
                            }
                        }
                    } catch (final NoClassDefFoundError e) {
                        LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "generateJunitTestClasses", e.getMessage(), e);
                    } catch (final ClassNotFoundException e) {
                        LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "generateJunitTestClasses", e.getMessage(), e);
                    }
                }
            }
            classLoaderForJar.close();
        } catch (final IOException e) {
            LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "generateJunitTestClasses", e.getMessage(), e);
        }
    }

    private void createFile(@NonNull final Class<?> crunchifyClass, @NonNull final String path) {
        try {
            String directoryPath = crunchifyClass.getPackage().getName();
            directoryPath = directoryPath.replace(".", "/");

            final File dir = new File(path + "/" + directoryPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            final Path file = Paths.get(path + "/" + directoryPath + "/" + crunchifyClass.getSimpleName() + "_TEST.java");

            final List<@NonNull String> javaLines = new ArrayList<>();

            final Set<String> allImports = getAllImports(crunchifyClass);

            javaLines.add("package " + crunchifyClass.getPackage().getName() + ";");
            javaLines.add("");
            javaLines.add("import org.junit.Test;");
            javaLines.add("");
            for (final String imports : allImports) {
                javaLines.add("import " + imports + ";");
            }
            javaLines.add("");
            javaLines.add("/**");
            javaLines.add(" * JUnit tests for the class " + crunchifyClass.getSimpleName() + ".");
            javaLines.add(" *");
            javaLines.add(" * @author JunitTestGenerator");
            javaLines.add(" */");
            javaLines.add("public class " + crunchifyClass.getSimpleName() + "_TEST {");
            javaLines.add("");

            final Constructor<?>[] declaredConstructors = crunchifyClass.getDeclaredConstructors();
            int i = 0;
            for (final Constructor<?> declaredConstructor : declaredConstructors) {
                if (declaredConstructor.isAccessible()) {
                    javaLines.add("   /**");
                    javaLines.add("    * " + crunchifyClass.getSimpleName() + ".");
                    final Class<?>[] params = declaredConstructor.getParameterTypes();
                    if (params != null && params.length > 0) {
                        javaLines.add("    * With params: ");
                        javaLines.add("    * <ul>");
                        for (final Class<?> class1 : params) {
                            javaLines.add("    *     <li>" + class1.getSimpleName() + "</li>");
                        }
                        javaLines.add("    * </ul>");
                    }
                    javaLines.add("    */");
                    javaLines.add("    @org.junit.Test");
                    javaLines.add("    public void test" + upperCaseFirst(crunchifyClass.getSimpleName()) + i + "() {");
                    String concatenatedParms = "";
                    if (params != null && params.length > 0) {
                        int j = 0;
                        for (final Class<?> class1 : params) {
                            if (class1.isPrimitive()) {
                                javaLines.add("        " + class1.getSimpleName() + " param" + j + ";");
                            } else {
                                javaLines.add("        " + class1.getSimpleName() + " param" + j + " = null;");
                            }
                            concatenatedParms = concatenatedParms + "param" + j + " ";
                            j++;
                        }
                        concatenatedParms = concatenatedParms.substring(0, concatenatedParms.length() - 1);
                        concatenatedParms = concatenatedParms.replaceAll(" ", ", ");
                    }
                    javaLines.add("        " + crunchifyClass.getSimpleName() + " " + lowerCaseFirst(crunchifyClass.getSimpleName()) + " = new " + crunchifyClass.getName() + "(" + concatenatedParms + ");");
                    javaLines.add("        ");
                    javaLines.add("        // Add assertions");
                    javaLines.add("        ");
                    javaLines.add("    }");
                    javaLines.add("");
                    i++;
                }
            }

            final Method[] declaredMethods = crunchifyClass.getDeclaredMethods();
            for (final Method declaredMethod : declaredMethods) {
                if (Modifier.isPublic(declaredMethod.getModifiers())) {
                    javaLines.add("   /**");
                    javaLines.add("    * " + declaredMethod.getName() + ".");
                    final Class<?>[] params = declaredMethod.getParameterTypes();
                    String concatenatedParams = "";
                    if (params != null && params.length > 0) {
                        javaLines.add("    * With params: ");
                        javaLines.add("    * <ul>");
                        for (final Class<?> class1 : params) {
                            javaLines.add("    *     <li>" + class1.getSimpleName() + "</li>");
                            String paramName = class1.getSimpleName();
                            if (class1.isArray()) {
                                paramName = "Array";
                            }
                            concatenatedParams = concatenatedParams + paramName;
                        }
                        javaLines.add("    * </ul>");
                    }
                    javaLines.add("    */");
                    javaLines.add("    @org.junit.Test");
                    if (concatenatedParams == "") {
                        javaLines.add("    public void test" + upperCaseFirst(declaredMethod.getName()) + "() {");
                    } else {
                        javaLines.add("    public void test" + upperCaseFirst(declaredMethod.getName()) + "With" + concatenatedParams + "() {");
                    }
                    String concatenatedParms = "";
                    if (params != null && params.length > 0) {
                        int j = 0;
                        for (final Class<?> class1 : params) {
                            if (class1.isPrimitive()) {
                                javaLines.add("        " + class1.getSimpleName() + " param" + j + ";");
                            } else {
                                javaLines.add("        " + class1.getSimpleName() + " param" + j + " = null;");
                            }
                            concatenatedParms = concatenatedParms + "param" + j + " ";
                            j++;
                        }
                        concatenatedParms = concatenatedParms.substring(0, concatenatedParms.length() - 1);
                        concatenatedParms = concatenatedParms.replaceAll(" ", ", ");
                    }
                    final boolean isStatic = Modifier.isStatic(declaredMethod.getModifiers());
                    if (!isStatic) {
                        javaLines.add("        " + crunchifyClass.getSimpleName() + " " + lowerCaseFirst(crunchifyClass.getSimpleName()) + " = null;");
                    }

                    final Class<?>[] declaredExceptionList = declaredMethod.getExceptionTypes();
                    String prefix = "        ";
                    if (declaredExceptionList != null && declaredExceptionList.length > 0) {
                        javaLines.add("        try {");
                        prefix = prefix + "    ";
                    }

                    final Class<?> returnMethod = declaredMethod.getReturnType();
                    if (returnMethod == null) {
                        if (isStatic) {
                            javaLines.add(prefix + crunchifyClass.getSimpleName() + "." + declaredMethod.getName() + "(" + concatenatedParms + ");");
                        } else {
                            javaLines.add(prefix + lowerCaseFirst(crunchifyClass.getSimpleName()) + "." + declaredMethod.getName() + "(" + concatenatedParms + ");");
                        }
                    } else {
                        if (isStatic) {
                            javaLines.add(prefix + returnMethod.getSimpleName() + " " + lowerCaseFirst(returnMethod.getSimpleName()) + "Result = " + crunchifyClass.getSimpleName() + "." + declaredMethod.getName() + "(" + concatenatedParms + ");");
                        } else {
                            javaLines.add(prefix + returnMethod.getSimpleName() + " " + lowerCaseFirst(returnMethod.getSimpleName()) + "Result = " + lowerCaseFirst(crunchifyClass.getSimpleName()) + "." + declaredMethod.getName() + "(" + concatenatedParms + ");");
                        }
                    }
                    javaLines.add(prefix);
                    javaLines.add(prefix + "// Add assertions");
                    javaLines.add(prefix);
                    if (declaredExceptionList != null && declaredExceptionList.length > 0) {
                        for (final Class<?> class1 : declaredExceptionList) {
                            javaLines.add("        } catch (" + class1.getSimpleName() + " e) {");
                            javaLines.add(prefix + " // Handle exception");
                        }
                        javaLines.add("        }");
                    }
                    javaLines.add("    }");
                    javaLines.add("");
                }
            }

            javaLines.add("");

            javaLines.add("}");

            Files.write(file, javaLines, Charset.forName("UTF-8"));
        } catch (final IOException e) {
            LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "createFile", e.getMessage(), e);
        }
    }

    private Set<String> getAllImports(@NonNull final Class<?> crunchifyClass) {
        final Set<String> result = new HashSet<>();

        final Constructor<?>[] declaredConstructors = crunchifyClass.getDeclaredConstructors();
        for (final Constructor<?> declaredConstructor : declaredConstructors) {
            final Class<?>[] params = declaredConstructor.getParameterTypes();
            if (params != null && params.length > 0) {
                for (final Class<?> class1 : params) {
                    if (!class1.isPrimitive() && !class1.isArray()) {
                        result.add(class1.getName());
                    }
                }
            }
        }

        final Method[] declaredMethods = crunchifyClass.getDeclaredMethods();
        for (final Method declaredMethod : declaredMethods) {
            final Class<?>[] params = declaredMethod.getParameterTypes();
            if (params != null && params.length > 0) {
                for (final Class<?> class1 : params) {
                    if (!class1.isPrimitive() && !class1.isArray()) {
                        result.add(class1.getName());
                    }
                }
            }
            final Class<?>[] declaredExceptionList = declaredMethod.getExceptionTypes();
            if (declaredExceptionList != null && declaredExceptionList.length > 0) {
                for (final Class<?> class1 : declaredExceptionList) {
                    if (!class1.isPrimitive() && !class1.isArray()) {
                        result.add(class1.getName());
                    }
                }
            }

            final Class<?> returnMethod = declaredMethod.getReturnType();
            if (returnMethod != null) {
                if (!returnMethod.isPrimitive() && !returnMethod.isArray()) {
                    result.add(returnMethod.getName());
                }
            }
        }

        return result;
    }

    private String lowerCaseFirst(final String value) {
        // Convert String to char array.
        final char[] array = value.toCharArray();
        // Modify first element in array.
        array[0] = Character.toLowerCase(array[0]);
        // Return string.
        return new String(array);
    }

    private String upperCaseFirst(final String value) {
        // Convert String to char array.
        final char[] array = value.toCharArray();
        // Modify first element in array.
        array[0] = Character.toUpperCase(array[0]);
        // Return string.
        return new String(array);
    }

    private Map<String, List<String>> getAllObjectListFromJar(@NonNull final String jarAbsolutePath) {
        final Map<String, List<String>> result = new HashMap<>();
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(jarAbsolutePath));
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    // This ZipEntry represents a class. Now, what class does it represent?
                    String className = entry.getName().replaceAll("/", "."); // including ".class"
                    if (className != null) {
                        className = className.substring(0, className.length() - ".class".length());
                        final String packageName = className.substring(0, className.lastIndexOf("."));
                        List<String> classNameList = result.get(packageName);
                        if (classNameList == null) {
                            classNameList = new ArrayList<>();
                            result.put(packageName, classNameList);
                        }
                        classNameList.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
                    }
                }
            }
            zip.close();
        } catch (final IOException e) {
            LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "getAllObjectListFromJar", e.getMessage(), e);
        }

        return result;
    }

    private Map<String, List<String>> getAllObjectListFromDirectory(@NonNull final String absolutePath) {
        final Map<String, List<String>> result = new HashMap<>();
        final List<@NonNull String> fileList = getFilesInPath(absolutePath);
        for (final String fileName : fileList) {
            // This ZipEntry represents a class. Now, what class does it represent?
            String className = fileName.replaceAll("/", ".").replaceAll("\\\\", "."); // including ".class"
            if (className != null) {
                className = className.substring(0, className.length() - ".class".length());
                final String packageName = className.substring(0, className.lastIndexOf("."));
                List<String> classNameList = result.get(packageName);
                if (classNameList == null) {
                    classNameList = new ArrayList<>();
                    result.put(packageName, classNameList);
                }
                classNameList.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
            }
        }

        return result;
    }

    @NonNull
    private List<@NonNull String> getFilesInPath(@NonNull final String path) {
        @NonNull
        final List<@NonNull String> result = new ArrayList<>();
        try {
            final File directory = new File(path);
            if (directory.isDirectory()) {
                final List<@NonNull File> files = (List<@NonNull File>) FileUtils.listFiles(directory, null, true);
                for (final @NonNull File currentFile : files) {
                    String fileName = currentFile.getAbsolutePath();
                    if (fileName != null && fileName.endsWith(".class") && !fileName.endsWith("package-info.class")) {
                        fileName = fileName.replace(path, "");
                        if (fileName.startsWith("\\")) {
                            fileName = fileName.substring(1);
                        }
                        result.add(fileName);
                    }
                }
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.logp(Level.SEVERE, "JunitTestGenerator", "getFilesInPath", e.getMessage(), e);
        }

        return result;
    }

}
