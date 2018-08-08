/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

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
