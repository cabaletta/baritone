package baritone.bot.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Brady
 * @since 8/3/2018 10:18 PM
 */
public final class GZIPUtils {

    private GZIPUtils() {}

    public static byte[] compress(byte[] in) {
        ByteArrayOutputStream byteStream = null;
        GZIPOutputStream gzipStream = null;

        try {
            byteStream = new ByteArrayOutputStream(in.length);
            gzipStream = new GZIPOutputStream(byteStream);
            gzipStream.write(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            // Clean up the byte array stream
            if (byteStream != null) {
                try {
                    byteStream.close();
                } catch (IOException ignored) {}
            }

            // Clean up the GZIP compression stream
            if (gzipStream != null) {
                try {
                    gzipStream.close();
                } catch (IOException ignored) {}
            }
        }

        return byteStream != null ? byteStream.toByteArray(): null;
    }

    public static byte[] decompress(byte[] in) {
        ByteArrayInputStream byteStream = null;
        GZIPInputStream gzipStream = null;
        ByteArrayOutputStream outStream = null;

        try {
            byteStream = new ByteArrayInputStream(in);
            gzipStream = new GZIPInputStream(byteStream);
            outStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            // Clean up the byte array stream
            if (byteStream != null) {
                try {
                    byteStream.close();
                } catch (IOException ignored) {}
            }

            // Clean up the GZIP compression stream
            if (gzipStream != null) {
                try {
                    gzipStream.close();
                } catch (IOException ignored) {}
            }

            // Clean up the byte output stream
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ignored) {}
            }
        }

        return outStream != null ? outStream.toByteArray() : null;
    }
}
