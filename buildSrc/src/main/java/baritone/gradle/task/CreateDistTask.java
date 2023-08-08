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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Brady
 * @since 10/12/2018
 */
public class CreateDistTask extends BaritoneGradleTask {

    private static MessageDigest SHA1_DIGEST;

    @TaskAction
    protected void exec() throws Exception {
        super.doFirst();
        super.verifyArtifacts();

        // Define the distribution file paths
        Path api = getRootRelativeFile("dist/" + getFileName(artifactApiPath));
        Path standalone = getRootRelativeFile("dist/" + getFileName(artifactStandalonePath));
        Path unoptimized = getRootRelativeFile("dist/" + getFileName(artifactUnoptimizedPath));

        // NIO will not automatically create directories
        Path dir = getRootRelativeFile("dist/");
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        // Copy build jars to dist/
        // TODO: dont copy files that dont exist
        Files.copy(this.artifactApiPath, api, REPLACE_EXISTING);
        Files.copy(this.artifactStandalonePath, standalone, REPLACE_EXISTING);
        Files.copy(this.artifactUnoptimizedPath, unoptimized, REPLACE_EXISTING);

        // Calculate all checksums and format them like "shasum"
        List<String> shasum = Files.list(getRootRelativeFile("dist/"))
                .filter(e -> e.getFileName().toString().endsWith(".jar"))
                .map(path -> sha1(path) + "  " + path.getFileName().toString())
                .collect(Collectors.toList());

        shasum.forEach(System.out::println);

        // Write the checksums to a file
        Files.write(getRootRelativeFile("dist/checksums.txt"), shasum);
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
