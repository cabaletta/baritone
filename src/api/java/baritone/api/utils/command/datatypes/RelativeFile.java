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

package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import static baritone.api.utils.Helper.HELPER;

public class RelativeFile implements IDatatypePost<File, File> {

    private final Path path;

    public RelativeFile() {
        path = null;
    }

    public RelativeFile(ArgConsumer consumer) {
        try {
            path = FileSystems.getDefault().getPath(consumer.getString());
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path");
        }
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return Stream.empty();
    }

    /**
     * Seriously
     *
     * @param file File
     * @return Canonical file of file
     * @author LoganDark
     */
    private static File getCanonicalFileUnchecked(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<String> tabComplete(ArgConsumer consumer, File base0) {
        // I will not make the caller deal with this, seriously
        // Tab complete code is beautiful and I'm not going to bloat it with dumb ass checked exception bullshit
        File base = getCanonicalFileUnchecked(base0);
        String currentPathStringThing = consumer.getString();
        Path currentPath = FileSystems.getDefault().getPath(currentPathStringThing);
        Path basePath = currentPath.isAbsolute() ? currentPath.getRoot() : base.toPath();
        boolean useParent = !currentPathStringThing.isEmpty() && !currentPathStringThing.endsWith(File.separator);
        File currentFile = currentPath.isAbsolute() ? currentPath.toFile() : new File(base, currentPathStringThing);
        return Arrays.stream(Objects.requireNonNull(getCanonicalFileUnchecked(
                useParent
                        ? currentFile.getParentFile()
                        : currentFile
        ).listFiles()))
                .map(f -> (currentPath.isAbsolute() ? f : basePath.relativize(f.toPath()).toString()) +
                        (f.isDirectory() ? File.separator : ""))
                .filter(s -> s.toLowerCase(Locale.US).startsWith(currentPathStringThing.toLowerCase(Locale.US)))
                .filter(s -> !s.contains(" "));
    }

    @Override
    public File apply(File original) {
        return getCanonicalFileUnchecked(original.toPath().resolve(path).toFile());
    }

    public static File gameDir() {
        File gameDir = HELPER.mc.gameDir.getAbsoluteFile();
        if (gameDir.getName().equals(".")) {
            return gameDir.getParentFile();
        }
        return gameDir;
    }
}
