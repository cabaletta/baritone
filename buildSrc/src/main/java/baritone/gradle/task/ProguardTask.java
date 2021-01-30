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
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Brady
 * @since 10/11/2018
 */
public class ProguardTask extends BaritoneGradleTask {

    @Input
    private String url;

    @Input
    private String extract;

    @TaskAction
    protected void exec() throws Exception {
        super.verifyArtifacts();

        // "Haha brady why don't you make separate tasks"
        processArtifact();
        downloadProguard();
        extractProguard();
        generateConfigs();
        proguardApi();
        proguardStandalone();
        cleanup();
    }

    private void processArtifact() throws Exception {
        if (Files.exists(this.artifactUnoptimizedPath)) {
            Files.delete(this.artifactUnoptimizedPath);
        }

        Determinizer.determinize(this.artifactPath.toString(), this.artifactUnoptimizedPath.toString());
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
        template.add(0, "-injars " + this.artifactPath.toString());
        template.add(1, "-outjars " + this.getTemporaryFile(PROGUARD_EXPORT_PATH));

        // Acquire the RT jar using "java -verbose". This doesn't work on Java 9+
        Process p = new ProcessBuilder(this.getJavaBinPathForProguard(), "-verbose").start();
        String out = IOUtils.toString(p.getInputStream(), "UTF-8").split("\n")[0].split("Opened ")[1].replace("]", "");
        template.add(2, "-libraryjars '" + out + "'");

        {
            final Stream<File> libraries;
            {
                // Discover all of the libraries that we will need to acquire from gradle
                final Stream<File> dependencies = acquireDependencies()
                    // remove MCP mapped jar
                    .filter(f -> !f.toString().endsWith("-recomp.jar"))
                    // go from the extra to the original downloaded client
                    .map(f -> f.toString().endsWith("client-extra.jar") ? new File(f.getParentFile(), "client.jar") : f);

                if (getProject().hasProperty("baritone.forge_build")) {
                    libraries = dependencies
                        .map(f -> f.toString().endsWith("client.jar") ? getSrgMcJar() : f);
                } else if (getProject().hasProperty("baritone.fabric_build")) {
                    libraries = dependencies
                        .map(f -> f.getName().endsWith("-v2.jar") && f.getName().startsWith("minecraft-") ? getSrgMcJar() : f);
                } else {
                    libraries = dependencies;
                }
            }
            libraries.forEach(f -> {
                template.add(2, "-libraryjars '" + f + "'");
            });
        }

        // API config doesn't require any changes from the changes that we made to the template
        Files.write(getTemporaryFile(PROGUARD_API_CONFIG), template);

        // For the Standalone config, don't keep the API package
        List<String> standalone = new ArrayList<>(template);
        standalone.removeIf(s -> s.contains("# this is the keep api"));
        Files.write(getTemporaryFile(PROGUARD_STANDALONE_CONFIG), standalone);
    }

    private File getSrgMcJar() {
        return getProject().getTasks().findByName("copyMcJar").getOutputs().getFiles().getSingleFile();
    }

    private Stream<File> acquireDependencies() {
        return getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().findByName("launch").getRuntimeClasspath().getFiles()
            .stream()
            .filter(File::isFile);
    }

    private void proguardApi() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_API_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactApiPath.toString());
    }

    private void proguardStandalone() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_STANDALONE_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactStandalonePath.toString());
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

        Path proguardJar = getTemporaryFile(PROGUARD_JAR);
        Process p = new ProcessBuilder("java", "-jar", proguardJar.toString(), "@" + config.toString())
                .directory(getTemporaryFile("").toFile()) // Set the working directory to the temporary folder]
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
