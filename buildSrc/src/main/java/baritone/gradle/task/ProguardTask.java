/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.gradle.task;

import baritone.gradle.util.Determinizer;
import baritone.gradle.util.MappingType;
import baritone.gradle.util.ReobfWrapper;
import org.apache.commons.io.IOUtils;
import org.gradle.api.JavaVersion;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.internal.plugins.DefaultConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.Pair;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.jvm.inspection.DefaultJvmVersionDetector;
import org.gradle.process.internal.DefaultExecActionFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Brady
 * @since 10/11/2018
 */
public class ProguardTask extends BaritoneGradleTask {

    private static final Pattern TEMP_LIBRARY_PATTERN = Pattern.compile("-libraryjars 'tempLibraries\\/([a-zA-Z0-9/_\\-\\.]+)\\.jar'");

    @Input
    private String url;

    @Input
    private String extract;

    private List<String> requiredLibraries;

    private File mixin;

    @TaskAction
    protected void exec() throws Exception {
        super.verifyArtifacts();

        // "Haha brady why don't you make separate tasks"
        processArtifact();
        downloadProguard();
        extractProguard();
        generateConfigs();
        acquireDependencies();
        proguardApi();
        proguardStandalone();
        cleanup();
    }

    private void processArtifact() throws Exception {
        if (Files.exists(this.artifactUnoptimizedPath)) {
            Files.delete(this.artifactUnoptimizedPath);
        }

        Determinizer.determinize(this.artifactPath.toString(), this.artifactUnoptimizedPath.toString(), Optional.empty());
    }

    private void downloadProguard() throws Exception {
        Path proguardZip = getTemporaryFile(PROGUARD_ZIP);
        if (!Files.exists(proguardZip)) {
            write(new URL(this.url).openStream(), proguardZip);
        }
    }

    private void extractProguard() throws Exception {
        Path proguardJar = getTemporaryFile(PROGUARD_JAR);
        if (!Files.exists(proguardJar)) {
            ZipFile zipFile = new ZipFile(getTemporaryFile(PROGUARD_ZIP).toFile());
            ZipEntry zipJarEntry = zipFile.getEntry(this.extract);
            write(zipFile.getInputStream(zipJarEntry), proguardJar);
            zipFile.close();
        }
    }

    private String getJavaBinPathForProguard() throws Exception {
        String path;
        try {
            path = findJavaPathByGradleConfig();
            if (path != null) return path;
        }
        catch (Exception ex) {
            System.err.println("Unable to find java by javaCompile options");
            ex.printStackTrace();
        }

        try {
            path = findJavaByJavaHome();
            if (path != null) return path;
        }
        catch(Exception ex) {
            System.err.println("Unable to find java by JAVA_HOME");
            ex.printStackTrace();
        }


        path = findJavaByGradleCurrentRuntime();
        if (path != null) return path;
        
        throw new Exception("Unable to find java to determine ProGuard libraryjars. Please specify forkOptions.executable in javaCompile," +
                " JAVA_HOME environment variable, or make sure to run Gradle with the correct JDK (a v1.8 only)");
    }

    private String findJavaByGradleCurrentRuntime() {
        String path = Jvm.current().getJavaExecutable().getAbsolutePath();

        if (this.validateJavaVersion(path)) {
            System.out.println("Using Gradle's runtime Java for ProGuard");
            return path;
        }
        return null;
    }

    private String findJavaByJavaHome() {
        final String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null) {

            String path = Jvm.forHome(new File(javaHomeEnv)).getJavaExecutable().getAbsolutePath();
            if (this.validateJavaVersion(path)) {
                System.out.println("Detected Java path by JAVA_HOME");
                return path;
            }
        }
        return null;
    }

    private String findJavaPathByGradleConfig() {
        final TaskCollection<JavaCompile> javaCompiles = super.getProject().getTasks().withType(JavaCompile.class);

        final JavaCompile compileTask = javaCompiles.iterator().next();
        final ForkOptions forkOptions = compileTask.getOptions().getForkOptions();

        if (forkOptions != null) {
            String javacPath = forkOptions.getExecutable();
            if (javacPath != null) {
                File javacFile = new File(javacPath);
                if (javacFile.exists()) {
                    File[] maybeJava = javacFile.getParentFile().listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.equals("java");
                        }
                    });

                    if (maybeJava != null && maybeJava.length > 0) {
                        String path = maybeJava[0].getAbsolutePath();
                        if (this.validateJavaVersion(path)) {
                            System.out.println("Detected Java path by forkOptions");
                            return path;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean validateJavaVersion(String java) {
        final JavaVersion javaVersion = new DefaultJvmVersionDetector(new DefaultExecActionFactory(new IdentityFileResolver())).getJavaVersion(java);

        if (!javaVersion.getMajorVersion().equals("8")) {
            System.out.println("Failed to validate Java version " + javaVersion.toString() + " [" + java + "] for ProGuard libraryjars");
            // throw new RuntimeException("Java version incorrect: " + javaVersion.getMajorVersion() + " for " + java);
            return false;
        }

        System.out.println("Validated Java version " + javaVersion.toString() + " [" + java + "] for ProGuard libraryjars");
        return true;
    }

    private void generateConfigs() throws Exception {
        Files.copy(getRelativeFile(PROGUARD_CONFIG_TEMPLATE), getTemporaryFile(PROGUARD_CONFIG_DEST), REPLACE_EXISTING);

        // Setup the template that will be used to derive the API and Standalone configs
        List<String> template = Files.readAllLines(getTemporaryFile(PROGUARD_CONFIG_DEST));
        template.add(0, "-injars '" + this.artifactPath.toString() + "'");
        template.add(1, "-outjars '" + this.getTemporaryFile(PROGUARD_EXPORT_PATH) + "'");

        // Acquire the RT jar using "java -verbose". This doesn't work on Java 9+
        Process p = new ProcessBuilder(this.getJavaBinPathForProguard(), "-verbose").start();
        String out = IOUtils.toString(p.getInputStream(), "UTF-8").split("\n")[0].split("Opened ")[1].replace("]", "");
        template.add(2, "-libraryjars '" + out + "'");
        template.add(3, "-libraryjars '" + Paths.get(out).resolveSibling("jce.jar") + "'");

        // API config doesn't require any changes from the changes that we made to the template
        Files.write(getTemporaryFile(PROGUARD_API_CONFIG), template);

        // For the Standalone config, don't keep the API package
        List<String> standalone = new ArrayList<>(template);
        standalone.removeIf(s -> s.contains("# this is the keep api"));
        Files.write(getTemporaryFile(PROGUARD_STANDALONE_CONFIG), standalone);

        // Discover all of the libraries that we will need to acquire from gradle
        this.requiredLibraries = new ArrayList<>();
        template.forEach(line -> {
            if (!line.startsWith("#")) {
                Matcher m = TEMP_LIBRARY_PATTERN.matcher(line);
                if (m.find()) {
                    this.requiredLibraries.add(m.group(1));
                }
            }
        });
    }

    private void acquireDependencies() throws Exception {

        // Create a map of all of the dependencies that we are able to access in this project
        // Likely a better way to do this, I just pair the dependency with the first valid configuration
        Map<String, Pair<Configuration, Dependency>> dependencyLookupMap = new HashMap<>();
        getProject().getConfigurations().stream().filter(Configuration::isCanBeResolved).forEach(config ->
                config.getAllDependencies().forEach(dependency ->
                        dependencyLookupMap.putIfAbsent(dependency.getName() + "-" + dependency.getVersion(), Pair.of(config, dependency))));

        // Create the directory if it doesn't already exist
        Path tempLibraries = getTemporaryFile(TEMP_LIBRARY_DIR);
        if (!Files.exists(tempLibraries)) {
            Files.createDirectory(tempLibraries);
        }

        // Iterate the required libraries to copy them to tempLibraries
        for (String lib : this.requiredLibraries) {
            // copy from the forgegradle cache
            if (lib.equals("minecraft")) {
                Path cachedJar = getMinecraftJar();
                Path inTempDir = getTemporaryFile("tempLibraries/minecraft.jar");
                // TODO: maybe try not to copy every time
                Files.copy(cachedJar, inTempDir, REPLACE_EXISTING);

                continue;
            }

            // Find a configuration/dependency pair that matches the desired library
            Pair<Configuration, Dependency> pair = null;
            for (Map.Entry<String, Pair<Configuration, Dependency>> entry : dependencyLookupMap.entrySet()) {
                if (entry.getKey().startsWith(lib)) {
                    pair = entry.getValue();
                }
            }

            // The pair must be non-null
            Objects.requireNonNull(pair);

            // Find the library jar file, and copy it to tempLibraries
            for (File file : pair.getLeft().files(pair.getRight())) {
                if (file.getName().startsWith(lib)) {
                    if (lib.contains("mixin")) {
                        mixin = file;
                    }
                    Files.copy(file.toPath(), getTemporaryFile("tempLibraries/" + lib + ".jar"), REPLACE_EXISTING);
                }
            }
        }
        if (mixin == null) {
            throw new IllegalStateException("Unable to find mixin jar");
        }
    }

    // a bunch of epic stuff to get the path to the cached jar
    private Path getMinecraftJar() throws Exception {
        MappingType mappingType;
        try {
            mappingType = getMappingType();
        } catch (Exception e) {
            System.err.println("Failed to get mapping type, assuming NOTCH.");
            mappingType = MappingType.NOTCH;
        }

        String suffix;
        switch (mappingType) {
            case NOTCH:
                suffix = "";
                break;
            case SEARGE:
                suffix = "-srgBin";
                break;
            case CUSTOM:
                throw new IllegalStateException("Custom mappings not supported!");
            default:
                throw new IllegalStateException("Unknown mapping type: " + mappingType);
        }

        DefaultConvention convention = (DefaultConvention) this.getProject().getConvention();
        Object extension = convention.getAsMap().get("minecraft");
        Objects.requireNonNull(extension);

        // for some reason cant use Class.forName
        Class<?> class_baseExtension = extension.getClass().getSuperclass().getSuperclass().getSuperclass(); // <-- cursed
        Field f_replacer = class_baseExtension.getDeclaredField("replacer");
        f_replacer.setAccessible(true);
        Object replacer = f_replacer.get(extension);
        Class<?> class_replacementProvider = replacer.getClass();
        Field replacement_replaceMap = class_replacementProvider.getDeclaredField("replaceMap");
        replacement_replaceMap.setAccessible(true);

        Map<String, Object> replacements = (Map) replacement_replaceMap.get(replacer);
        String cacheDir = replacements.get("CACHE_DIR").toString() + "/net/minecraft";
        String mcVersion = replacements.get("MC_VERSION").toString();
        String mcpInsert = replacements.get("MAPPING_CHANNEL").toString() + "/" + replacements.get("MAPPING_VERSION").toString();
        String fullJarName = "minecraft-" + mcVersion + suffix + ".jar";

        String baseDir = String.format("%s/minecraft/%s/", cacheDir, mcVersion);

        String jarPath;
        if (mappingType == MappingType.SEARGE) {
            jarPath = String.format("%s/%s/%s", baseDir, mcpInsert, fullJarName);
        } else {
            jarPath = baseDir + fullJarName;
        }
        jarPath = jarPath
                .replace("/", File.separator)
                .replace("\\", File.separator); // hecking regex

        return new File(jarPath).toPath();
    }

    // throws IllegalStateException if mapping type is ambiguous or it fails to find it
    private MappingType getMappingType() {
        // if it fails to find this then its probably a forgegradle version problem
        Set<Object> reobf = (NamedDomainObjectContainer<Object>) this.getProject().getExtensions().getByName("reobf");

        List<MappingType> mappingTypes = getUsedMappingTypes(reobf);
        long mappingTypesUsed = mappingTypes.size();
        if (mappingTypesUsed == 0) {
            throw new IllegalStateException("Failed to find mapping type (no jar task?)");
        }
        if (mappingTypesUsed > 1) {
            throw new IllegalStateException("Ambiguous mapping type (multiple jars with different mapping types?)");
        }

        return mappingTypes.get(0);
    }

    private List<MappingType> getUsedMappingTypes(Set<Object> reobf) {
        return reobf.stream()
                .map(ReobfWrapper::new)
                .map(ReobfWrapper::getMappingType)
                .distinct()
                .collect(Collectors.toList());
    }

    private void proguardApi() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_API_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactApiPath.toString(), Optional.empty());
        Determinizer.determinize(this.proguardOut.toString(), this.artifactForgeApiPath.toString(), Optional.of(mixin));
    }

    private void proguardStandalone() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_STANDALONE_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactStandalonePath.toString(), Optional.empty());
        Determinizer.determinize(this.proguardOut.toString(), this.artifactForgeStandalonePath.toString(), Optional.of(mixin));
    }

    private void cleanup() {
        try {
            Files.delete(this.proguardOut);
        } catch (IOException ignored) {}
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setExtract(String extract) {
        this.extract = extract;
    }

    private void runProguard(Path config) throws Exception {
        // Delete the existing proguard output file. Proguard probably handles this already, but why not do it ourselves
        if (Files.exists(this.proguardOut)) {
            Files.delete(this.proguardOut);
        }

        // Make paths relative to work directory; fixes spaces in path to config, @"" doesn't work
        Path workingDirectory = getTemporaryFile("");
        Path proguardJar = workingDirectory.relativize(getTemporaryFile(PROGUARD_JAR));
        config = workingDirectory.relativize(config);
        
        // Honestly, if you still have spaces in your path at this point, you're SOL.

        Process p = new ProcessBuilder("java", "-jar", proguardJar.toString(), "@" + config.toString())
                .directory(workingDirectory.toFile()) // Set the working directory to the temporary folder]
                .start();

        // We can't do output inherit process I/O with gradle for some reason and have it work, so we have to do this
        this.printOutputLog(p.getInputStream(), System.out);
        this.printOutputLog(p.getErrorStream(), System.err);

        // Halt the current thread until the process is complete, if the exit code isn't 0, throw an exception
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Proguard exited with code " + exitCode);
        }
    }

    private void printOutputLog(InputStream stream, PrintStream outerr) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outerr.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
