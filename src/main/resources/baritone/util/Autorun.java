package baritone.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import baritone.util.Out;
import net.minecraft.client.main.Main;

/**
 *
 * @author avecowa
 */
public class Autorun {
    public static void start(String[] args) {
        Out.log(Arrays.asList(args));
        Out.log(System.getProperty("java.library.path"));
        //System.setProperty("java.library.path", System.getProperty("user.home") + "/Dropbox/Baritone/mcp918/jars/versions/1.8.8/1.8.8-natives/");
        //Out.log(System.getProperty("java.library.path"));
        Main.main(concat(new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", "assets", "--assetIndex", "1.8", "--userProperties", "{}"}, args));
    }
    public static void runprocess(String s) throws IOException {
        Process p;
        Out.log(p = Runtime.getRuntime().exec(s));
        InputStream i = p.getInputStream();
        InputStream e = p.getErrorStream();
        OutputStream o = p.getOutputStream();
        while (p.isAlive() || e.available() > 0 || i.available() > 0) {
            while (i.available() > 0) {
                System.out.print((char) (i.read()));
            }
            while (e.available() > 0) {
                System.out.print((char) (e.read()));
            }
        }
    }
    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
