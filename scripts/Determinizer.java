import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * Make a .jar file deterministic by sorting all entries by name, and setting all the last modified times to 0.
 * This makes the build 100% reproducible since the timestamp when you built it no longer affects the final file.
 * @author leijurv
 */
public class Determinizer {

    public static void main(String[] args) throws IOException {
        System.out.println("Input path: " + args[0] + " Output path: " + args[1]);
        JarFile jarFile = new JarFile(new File(args[0]));
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(new File(args[1])));
        ArrayList<JarEntry> entries = jarFile.stream().collect(Collectors.toCollection(ArrayList::new));
        entries.sort(Comparator.comparing(JarEntry::getName));
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
        jarFile.close();
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }
}
