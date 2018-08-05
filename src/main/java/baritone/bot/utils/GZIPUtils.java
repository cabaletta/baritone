package baritone.bot.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Brady
 * @since 8/3/2018 10:18 PM
 */
public final class GZIPUtils {

    private GZIPUtils() {
    }

    public static byte[] compress(byte[] in) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(in.length);

        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(in);
        }
        return byteStream.toByteArray();
    }

    public static byte[] decompress(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try (GZIPInputStream gzipStream = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
        }
        return outStream.toByteArray();
    }
}
