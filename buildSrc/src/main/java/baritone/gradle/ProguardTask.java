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

package baritone.gradle;

import baritone.gradle.util.Determinizer;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.Pair;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Brady
 * @since 10/11/2018
 */
public class ProguardTask extends DefaultTask {

    private static final JsonParser PARSER = new JsonParser();

    private static final Pattern TEMP_LIBRARY_PATTERN = Pattern.compile("-libraryjars 'tempLibraries\\/([a-zA-Z0-9/_\\-\\.]+)\\.jar'");

    private static final String
            PROGUARD_ZIP                    = "proguard.zip",
            PROGUARD_JAR                    = "proguard.jar",
            PROGUARD_CONFIG_TEMPLATE        = "scripts/proguard.pro",
            PROGUARD_CONFIG_DEST            = "template.pro",
            PROGUARD_API_CONFIG             = "api.pro",
            PROGUARD_STANDALONE_CONFIG      = "standalone.pro",
            PROGUARD_EXPORT_PATH            = "proguard_out.jar",

            VERSION_MANIFEST = "version_manifest.json",

            TEMP_LIBRARY_DIR = "tempLibraries/",

            ARTIFACT_UNOPTIMIZED = "%s-unoptimized-%s.jar",
            ARTIFACT_API         = "%s-api-%s.jar",
            ARTIFACT_STANDALONE  = "%s-standalone-%s.jar";

    @Input
    private String url;

    @Input
    private String extract;

    @Input
    private String versionManifest;

    private String artifactName, artifactVersion;
    private Path artifactPath, artifactUnoptimizedPath, artifactApiPath, artifactStandalonePath, proguardOut;
    private Map<String, String> versionDownloadMap;
    private List<String> requiredLibraries;

    @TaskAction
    private void exec() throws Exception {
        // "Haha brady why don't you make separate tasks"
        verifyArtifacts();
        processArtifact();
        downloadProguard();
        extractProguard();
        generateConfigs();
        downloadVersionManifest();
        acquireDependencies();
        proguardApi();
        proguardStandalone();
        cleanup();
    }

    private void verifyArtifacts() throws Exception {
        this.artifactName = getProject().getName();
        this.artifactVersion = getProject().getVersion().toString();

        // The compiled baritone artifact that is exported when the build task is ran
        String artifactName = String.format("%s-%s.jar", this.artifactName, this.artifactVersion);

        this.artifactPath            = this.getBuildFile(artifactName);
        this.artifactUnoptimizedPath = this.getBuildFile(String.format(ARTIFACT_UNOPTIMIZED, this.artifactName, this.artifactVersion));
        this.artifactApiPath         = this.getBuildFile(String.format(ARTIFACT_API,         this.artifactName, this.artifactVersion));
        this.artifactStandalonePath  = this.getBuildFile(String.format(ARTIFACT_STANDALONE,  this.artifactName, this.artifactVersion));

        this.proguardOut = this.getTemporaryFile(PROGUARD_EXPORT_PATH);

        if (!Files.exists(this.artifactPath)) {
            throw new Exception("Artifact not found! Run build first!");
        }
    }

    private void processArtifact() throws Exception {
        if (Files.exists(this.artifactUnoptimizedPath)) {
            Files.delete(this.artifactUnoptimizedPath);
        }

        Determinizer.main(this.artifactPath.toString(), this.artifactUnoptimizedPath.toString());
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

    private void generateConfigs() throws Exception {
        Files.copy(getRelativeFile(PROGUARD_CONFIG_TEMPLATE), getTemporaryFile(PROGUARD_CONFIG_DEST), REPLACE_EXISTING);

        // Setup the template that will be used to derive the API and Standalone configs
        List<String> template = Files.readAllLines(getTemporaryFile(PROGUARD_CONFIG_DEST));
        template.removeIf(s -> s.endsWith("# this is the rt jar") || s.startsWith("-injars") || s.startsWith("-outjars"));
        template.add(0, "-injars " + this.artifactPath.toString());
        template.add(1, "-outjars " + this.getTemporaryFile(PROGUARD_EXPORT_PATH));

        // Acquire the RT jar using "java -verbose". This doesn't work on Java 9+
        Process p = new ProcessBuilder("java", "-verbose").start();
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

    private void downloadVersionManifest() throws Exception {
        Path manifestJson = getTemporaryFile(VERSION_MANIFEST);
        write(new URL(this.versionManifest).openStream(), manifestJson);

        // Place all the versions in the map with their download URL
        this.versionDownloadMap = new HashMap<>();
        JsonObject json = readJson(Files.readAllLines(manifestJson)).getAsJsonObject();
        JsonArray versions = json.getAsJsonArray("versions");
        versions.forEach(element -> {
            JsonObject object = element.getAsJsonObject();
            this.versionDownloadMap.put(object.get("id").getAsString(), object.get("url").getAsString());
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
            // Download the version jar from the URL acquired from the version manifest
            if (lib.startsWith("minecraft")) {
                String version = lib.split("-")[1];
                Path versionJar = getTemporaryFile("tempLibraries/" + lib + ".jar");
                if (!Files.exists(versionJar)) {
                    write(new URL(this.versionDownloadMap.get(version)).openStream(), versionJar);
                }
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
                    Files.copy(file.toPath(), getTemporaryFile("tempLibraries/" + lib + ".jar"), REPLACE_EXISTING);
                }
            }
        }
    }

    private void proguardApi() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_API_CONFIG));
        Determinizer.main(this.proguardOut.toString(), this.artifactApiPath.toString());
    }

    private void proguardStandalone() throws Exception {
        runProguard(getTemporaryFile(PROGUARD_STANDALONE_CONFIG));
        Determinizer.main(this.proguardOut.toString(), this.artifactStandalonePath.toString());
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

    public void setVersionManifest(String versionManifest) {
        this.versionManifest = versionManifest;
    }

    /*
     * A LOT OF SHITTY UTIL METHODS ARE BELOW.
     *
     * PROCEED WITH CAUTION
     */

    private void runProguard(Path config) throws Exception {
        // Delete the existing proguard output file. Proguard probably handles this already, but why not do it ourselves
        if (Files.exists(this.proguardOut)) {
            Files.delete(this.proguardOut);
        }

        Path proguardJar = getTemporaryFile(PROGUARD_JAR);
        Process p = new ProcessBuilder("java", "-jar", proguardJar.toString(), "@" + config.toString())
                .directory(getTemporaryFile("").toFile()) // Set the working directory to the temporary folder
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        // Halt the current thread until the process is complete, if the exit code isn't 0, throw an exception
        int exitCode;
        if ((exitCode = p.waitFor()) != 0) {
            throw new Exception("Proguard exited with code " + exitCode);
        }
    }

    private void write(InputStream stream, Path file) throws Exception {
        if (Files.exists(file)) {
            Files.delete(file);
        }
        Files.copy(stream, file);
    }

    private Path getRelativeFile(String file) {
        return Paths.get(new File(file).getAbsolutePath());
    }

    private Path getTemporaryFile(String file) {
        return Paths.get(new File(getTemporaryDir(), file).getAbsolutePath());
    }

    private Path getBuildFile(String file) {
        return getRelativeFile("build/libs/" + file);
    }

    private JsonElement readJson(List<String> lines) {
        return PARSER.parse(String.join("\n", lines));
    }
}
