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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Brady
 * @since 10/12/2018
 */
public abstract class CreateDistTask extends BaritoneGradleTask {

    private static MessageDigest SHA1_DIGEST;

    @InputFile
    abstract public RegularFileProperty getArtifactApiPath();

    @InputFile
    abstract public RegularFileProperty getArtifactStandalonePath();

    @InputFile
    abstract public RegularFileProperty getArtifactUnoptimizedPath();

    @TaskAction
    protected void exec() throws Exception {
        super.doFirst();

        // Define the distribution file paths
        Path api = getRootRelativeFile("dist/" + getFileName(getArtifactApiPath().get().getAsFile().toPath()));
        Path standalone = getRootRelativeFile("dist/" + getFileName(getArtifactStandalonePath().get().getAsFile().toPath()));
        Path unoptimized = getRootRelativeFile("dist/" + getFileName(getArtifactUnoptimizedPath().get().getAsFile().toPath()));

        // NIO will not automatically create directories
        Path dir = getRootRelativeFile("dist/");
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        // Copy build jars to dist/
        // TODO: dont copy files that dont exist
        Files.copy(getArtifactApiPath().get().getAsFile().toPath(), api, REPLACE_EXISTING);
        Files.copy(getArtifactStandalonePath().get().getAsFile().toPath(), standalone, REPLACE_EXISTING);
        Files.copy(getArtifactUnoptimizedPath().get().getAsFile().toPath(), unoptimized, REPLACE_EXISTING);

        // Calculate all checksums and format them like "shasum"
        List<String> shasum = Stream.of(getArtifactApiPath().get().getAsFile().toPath(), getArtifactStandalonePath().get().getAsFile().toPath(), getArtifactUnoptimizedPath().get().getAsFile().toPath())
                .filter(e -> e.getFileName().toString().endsWith(".jar"))
                .map(path -> sha1(path) + "  " + path.getFileName().toString())
                .collect(Collectors.toList());

        shasum.forEach(System.out::println);

        // Write the checksums to a file
        Files.write(getRootRelativeFile("dist/checksums.txt"), shasum, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String getFileName(Path p) {
        return p.getFileName().toString();
    }

    private static synchronized String sha1(Path path) {
        try {
            if (SHA1_DIGEST == null) {
                SHA1_DIGEST = MessageDigest.getInstance("SHA-1");
            }
            return bytesToHex(SHA1_DIGEST.digest(Files.readAllBytes(path))).toLowerCase();
        } catch (Exception e) {
            // haha no thanks
            throw new IllegalStateException(e);
        }
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
