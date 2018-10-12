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

package baritone.gradle.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * Make a .jar file deterministic by sorting all entries by name, and setting all the last modified times to 0.
 * This makes the build 100% reproducible since the timestamp when you built it no longer affects the final file.
 *
 * @author leijurv
 */
public class Determinizer {

    public static void main(String... args) throws IOException {
        /*
        haha yeah can't relate

        unzip -p build/libs/baritone-$VERSION.jar "mixins.baritone.refmap.json" | jq --sort-keys -c -M '.' > mixins.baritone.refmap.json
        zip -u build/libs/baritone-$VERSION.jar mixins.baritone.refmap.json
        rm mixins.baritone.refmap.json
         */

        System.out.println("Running Determinizer");
        System.out.println(" Input path: " + args[0]);
        System.out.println(" Output path: " + args[1]);

        JarFile jarFile = new JarFile(new File(args[0]));
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(new File(args[1])));

        List<JarEntry> entries = jarFile.stream()
                .sorted(Comparator.comparing(JarEntry::getName))
                .collect(Collectors.toList());

        for (JarEntry entry : entries) {
            if (entry.getName().equals("META-INF/fml_cache_annotation.json")) {
                continue;
            }
            if (entry.getName().equals("META-INF/fml_cache_class_versions.json")) {
                continue;
            }
            JarEntry clone = new JarEntry(entry.getName());
            clone.setTime(0);
            jos.putNextEntry(clone);
            copy(jarFile.getInputStream(entry), jos);
        }

        jos.finish();
        jos.close();
        jarFile.close();
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }
}
