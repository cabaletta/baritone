package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class RelativeFile implements IDatatypePost<File, File> {
    private final Path path;

    public RelativeFile() {
        path = null;
    }

    public RelativeFile(ArgConsumer consumer) {
        try {
            path = FileSystems.getDefault().getPath(consumer.getString());
        } catch (InvalidPathException e) {
            throw new RuntimeException("invalid path");
        }
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return Stream.empty();
    }

    public static Stream<String> tabComplete(ArgConsumer consumer, File base) {
        String currentPathStringThing = consumer.getString();
        Path currentPath = FileSystems.getDefault().getPath(currentPathStringThing);
        Path basePath = currentPath.isAbsolute() ? currentPath.getRoot() : base.toPath();
        boolean useParent = !currentPathStringThing.isEmpty() && !currentPathStringThing.endsWith(File.separator);
        File currentFile = currentPath.isAbsolute() ? currentPath.toFile() : new File(base, currentPathStringThing);

        return Arrays.stream(Objects.requireNonNull((useParent ? currentFile.getParentFile() : currentFile).listFiles()))
            .map(f -> (currentPath.isAbsolute() ? f : basePath.relativize(f.toPath()).toString()) +
                (f.isDirectory() ? File.separator : ""))
            .filter(s -> s.toLowerCase(Locale.US).startsWith(currentPathStringThing.toLowerCase(Locale.US)));
    }

    @Override
    public File apply(File original) {
        return original.toPath().resolve(path).toFile();
    }
}
