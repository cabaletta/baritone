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
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.jvm.Jvm;

import java.io.*;
import java.net.MalformedURLException;
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

    public String getUrl() {
        return url;
    }

    @Input
    private String extract;

    public String getExtract() {
        return extract;
    }

    @Input
    private String compType;

    public String getCompType() {
        return compType;
    }

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

    private boolean isMcJar(File f) {
        return f.getName().startsWith(compType.equals("FORGE") ? "forge-" : "minecraft-") && f.getName().contains("minecraft-merged-named");
    }

    private File getMcJar() throws IOException {
        File mcClientJar = this.getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().findByName("main").getCompileClasspath().getFiles()
            .stream()
            .filter(this::isMcJar)
            .map(f -> {
                switch (compType) {
                    case "OFFICIAL":
                        return new File(f.getParentFile().getParentFile(), "minecraft-merged.jar");
                    case "FABRIC":
                        return new File(f.getParentFile().getParentFile(), "minecraft-merged-intermediary.jar");
                    case "FORGE":
                        return new File(f.getParentFile().getParentFile(), f.getName().replace("-named.jar", "-srg.jar"));
                }
                return null;
                })
            .findFirst()
            .get();
        if (!mcClientJar.exists()) throw new IOException("Failed to find minecraft! " + mcClientJar.getAbsolutePath());
        return mcClientJar;
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
        //TODO: fix for j16
//        final JavaVersion javaVersion = new DefaultJvmVersionDetector(new DefaultExecActionFactory(new IdentityFileResolver())).getJavaVersion(java);
//
//        if (!javaVersion.getMajorVersion().equals("8")) {
//            System.out.println("Failed to validate Java version " + javaVersion.toString() + " [" + java + "] for ProGuard libraryjars");
//            // throw new RuntimeException("Java version incorrect: " + javaVersion.getMajorVersion() + " for " + java);
//            return false;
//        }
//
//        System.out.println("Validated Java version " + javaVersion.toString() + " [" + java + "] for ProGuard libraryjars");
        return true;
    }

    private void generateConfigs() throws Exception {
        Files.copy(getRootRelativeFile(PROGUARD_CONFIG_TEMPLATE), getTemporaryFile(PROGUARD_CONFIG_DEST), REPLACE_EXISTING);

        // Setup the template that will be used to derive the API and Standalone configs
        List<String> template = Files.readAllLines(getTemporaryFile(PROGUARD_CONFIG_DEST));
        template.add(0, "-injars " + this.artifactPath.toString());
        template.add(1, "-outjars " + this.getTemporaryFile(PROGUARD_EXPORT_PATH));

        template.add(2, "-libraryjars  <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)");
        template.add(3, "-libraryjars  <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)");

        {
            final Stream<File> libraries;
            File mcJar;
            try {
                mcJar = getMcJar();
            } catch (Exception e) {
                throw new RuntimeException("Failed to find Minecraft jar", e);
            }

            {
                // Discover all of the libraries that we will need to acquire from gradle
                final Stream<File> dependencies = acquireDependencies()
                    // remove MCP mapped jar, and nashorn
                    .filter(f -> !f.toString().endsWith("-recomp.jar") && !f.getName().startsWith("nashorn") && !f.getName().startsWith("coremods"));

                libraries = dependencies
                    .map(f -> isMcJar(f) ? mcJar : f);
            }
            libraries.forEach(f -> {
                template.add(2, "-libraryjars '" + f + "'");
            });
        }

        Files.createDirectories(this.getRootRelativeFile(PROGUARD_MAPPING_DIR));

        List<String> api = new ArrayList<>(template);
        api.add(2, "-printmapping " + new File(this.getRootRelativeFile(PROGUARD_MAPPING_DIR).toFile(), "mappings-" + compType + "-api.txt"));

        // API config doesn't require any changes from the changes that we made to the template
        Files.write(getTemporaryFile(compType+PROGUARD_API_CONFIG), api);

        // For the Standalone config, don't keep the API package
        List<String> standalone = new ArrayList<>(template);
        standalone.removeIf(s -> s.contains("# this is the keep api"));
        standalone.add(2, "-printmapping " + new File(this.getRootRelativeFile(PROGUARD_MAPPING_DIR).toFile(), "mappings-" + compType + "-standalone.txt"));
        Files.write(getTemporaryFile(compType+PROGUARD_STANDALONE_CONFIG), standalone);
    }

    private Stream<File> acquireDependencies() {
        return getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().findByName("main").getCompileClasspath().getFiles()
            .stream()
            .filter(File::isFile);
    }

    private void proguardApi() throws Exception {
        runProguard(getTemporaryFile(compType+PROGUARD_API_CONFIG));
        Determinizer.determinize(this.proguardOut.toString(), this.artifactApiPath.toString());
    }

    private void proguardStandalone() throws Exception {
        runProguard(getTemporaryFile(compType+PROGUARD_STANDALONE_CONFIG));
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

    public void setCompType(String compType) {
        this.compType = compType;
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
