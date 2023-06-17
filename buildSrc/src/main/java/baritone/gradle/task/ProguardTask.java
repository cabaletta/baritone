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
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.jvm.Jvm;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    private File pathfinder;

    @TaskAction
    protected void exec() throws Exception {
        super.verifyArtifacts();

        // "Haha brady why don't you make separate tasks"
        downloadProguard();
        extractProguard();
        generateConfigs();
        acquireDependencies();
        processArtifact();
        proguardApi();
        proguardStandalone();
        cleanup();
    }

    private void processArtifact() throws Exception {
        if (Files.exists(this.artifactUnoptimizedPath)) {
            Files.delete(this.artifactUnoptimizedPath);
        }

        Determinizer.determinize(this.artifactPath.toString(), this.artifactUnoptimizedPath.toString(), Arrays.asList(pathfinder), false);
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
        } catch (Exception ex) {
            System.err.println("Unable to find java by javaCompile options");
            ex.printStackTrace();
        }

        try {
            path = findJavaByJavaHome();
            if (path != null) return path;
        } catch (Exception ex) {
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
        System.out.println("Using Gradle's runtime Java for ProGuard");
        return path;
    }

    private String findJavaByJavaHome() {
        final String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null) {
            String path = Jvm.forHome(new File(javaHomeEnv)).getJavaExecutable().getAbsolutePath();
            System.out.println("Detected Java path by JAVA_HOME");
            return path;
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
                    File[] maybeJava = javacFile.getParentFile().listFiles((dir, name) -> name.equals("java"));
                    if (maybeJava != null && maybeJava.length > 0) {
                        String path = maybeJava[0].getAbsolutePath();
                        System.out.println("Detected Java path by forkOptions");
                        return path;
                    }
                }
            }
        }
        return null;
    }

    private void generateConfigs() throws Exception {
        Files.copy(getRelativeFile(PROGUARD_CONFIG_TEMPLATE), getTemporaryFile(PROGUARD_CONFIG_DEST), StandardCopyOption.REPLACE_EXISTING);

        // Setup the template that will be used to derive the API and Standalone configs
        List<String> template = Files.readAllLines(getTemporaryFile(PROGUARD_CONFIG_DEST));
        template.add(0, "-injars '" + this.artifactPath.toString() + "'");
        template.add(1, "-outjars '" + this.getTemporaryFile(PROGUARD_EXPORT_PATH) + "'");

        // Acquire the RT jar using "java -verbose". This doesn't work on Java 9+
        Process p = new ProcessBuilder(this.getJavaBinPathForProguard(), "-verbose").start();
        String out = IOUtils.toString(p.getInputStream(), "UTF-8").split("\n")[0].split("Opened ")[1].replace("]", "");
        template.add(2, "-libraryjars '" + out + "'");

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

    private static final class Pair<A, B> {
        public final A a;
        public final B b;

        private Pair(final A a, final B b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "a=" + this.a +
                    ", " +
                    "b=" + this.b +
                    '}';
        }
    }

    private void acquireDependencies() throws Exception {
        // Create a map of all of the dependencies that we are able to access in this project
        // Likely a better way to do this, I just pair the dependency with the first valid configuration
        Map<String, Pair<Configuration, Dependency>> dependencyLookupMap = new HashMap<>();
        Map<String, File> files = new HashMap<>();
        getProject().getConfigurations().stream().filter(Configuration::isCanBeResolved).forEach(config -> {
            for (File file : config.getFiles()) {
                files.put(file.getName(), file);
            }
            config.getAllDependencies().forEach(dependency ->
                    dependencyLookupMap.putIfAbsent(dependency.getName() + "-" + dependency.getVersion(), new Pair<>(config, dependency)));
        });

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
                Files.copy(cachedJar, inTempDir, StandardCopyOption.REPLACE_EXISTING);

                continue;
            }

            // Find a configuration/dependency pair that matches the desired library
            Pair<Configuration, Dependency> pair = null;
            for (Map.Entry<String, Pair<Configuration, Dependency>> entry : dependencyLookupMap.entrySet()) {
                if (entry.getKey().startsWith(lib)) {
                    pair = entry.getValue();
                }
            }
            // Find the library jar file, and copy it to tempLibraries
            if (pair == null) {
                File libFile = files.get(lib + ".jar");
                if (libFile == null) {
                    libFile = files.values().stream().filter(file -> file.getName().startsWith(lib)).findFirst().orElse(null);
                    if (libFile == null) {
                        throw new IllegalStateException(lib);
                    }
                }
                copyTempLib(lib, libFile);
            } else {
                for (File file : pair.a.files(pair.b)) {
                    if (file.getName().startsWith(lib)) {
                        copyTempLib(lib, file);
                    }
                }
            }
        }
        if (mixin == null) {
            throw new IllegalStateException("Unable to find mixin jar");
        }
        if (pathfinder == null) {
            throw new IllegalStateException("Unable to find pathfinder jar");
        }
    }

    private void copyTempLib(String lib, File libFile) throws IOException {
        if (lib.contains("mixin")) {
            mixin = libFile;
        }
        if (lib.contains("nether-pathfinder")) {
            pathfinder = libFile;
        }
        Files.copy(libFile.toPath(), getTemporaryFile("tempLibraries/" + lib + ".jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    // a bunch of epic stuff to get the path to the cached jar
    private Path getMinecraftJar() throws Exception {
        return getObfuscatedMinecraftJar(getProject(), false); // always notch jar for now.
    }

    private static Path getObfuscatedMinecraftJar(final Project project, final boolean srg) throws Exception {
        final Object extension = Objects.requireNonNull(project.getExtensions().findByName("minecraft"), "Unable to find Minecraft extension.");

        final Class<?> mcpRepoClass = mcpRepoClass(extension.getClass().getClassLoader());
        final Field mcpRepoInstanceField = mcpRepoClass.getDeclaredField("INSTANCE");
        mcpRepoInstanceField.setAccessible(true);
        final Method findMethod = mcpRepoClass.getDeclaredMethod(srg ? "findSrg" : "findRaw", String.class, String.class);
        findMethod.setAccessible(true);

        final Object mcpRepo = mcpRepoInstanceField.get(null);
        final String mcpVersion = (String) Objects.requireNonNull(project.getExtensions().getExtraProperties().get("MCP_VERSION"), "Extra property \"MCP_VERSION\" not found");
        return ((File) findMethod.invoke(mcpRepo, "joined", mcpVersion)).toPath();
    }

    private static Class<?> mcpRepoClass(final ClassLoader loader) throws Exception {
        final Method forName0 = Class.class.getDeclaredMethod("forName0", String.class, boolean.class, ClassLoader.class, Class.class);
        forName0.setAccessible(true);
        return (Class<?>) forName0.invoke(null, "net.minecraftforge.gradle.mcp.MCPRepo", true, loader, null);
    }

    private void proguardApi() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_API_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactApiPath.toString(), Arrays.asList(pathfinder), false);
        Determinizer.determinize(this.proguardOut.toString(), this.artifactForgeApiPath.toString(), Arrays.asList(pathfinder, mixin), true);
    }

    private void proguardStandalone() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_STANDALONE_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactStandalonePath.toString(), Arrays.asList(pathfinder), false);
        Determinizer.determinize(this.proguardOut.toString(), this.artifactForgeStandalonePath.toString(), Arrays.asList(pathfinder, mixin), true);
    }

    private void cleanup() {
        try {
            Files.delete(this.proguardOut);
        } catch (IOException ignored) {}
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setExtract(String extract) {
        this.extract = extract;
    }

    public String getExtract() {
        return extract;
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
            Thread.sleep(1000);
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
