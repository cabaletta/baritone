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

package baritone.process.elytra.pathfinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** What the native library answered, written by java/oracle/oracle.cpp. */
final class Oracle {

    static final long SEED = 146008555100680L;

    static final class Ray {
        final double[] from = new double[3];
        final double[] to = new double[3];
        boolean hit;
        final double[] where = new double[3];
    }

    private Oracle() {}

    static long fnv1a(byte[] data) {
        long h = 0xcbf29ce484222325L;
        for (byte b : data) {
            h ^= (b & 0xFF);
            h *= 0x100000001b3L;
        }
        return h;
    }

    /** "x z hash" per line. */
    static List<String[]> chunkHashes() throws IOException {
        final List<String[]> out = new ArrayList<>();
        try (InputStream in = Oracle.class.getResourceAsStream("/chunk-hashes.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) out.add(line.split(" "));
            }
        }
        return out;
    }

    static List<Ray> rays() throws IOException {
        final byte[] bytes;
        try (InputStream in = Oracle.class.getResourceAsStream("/rays.bin")) {
            final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            final byte[] chunk = new byte[1 << 16];
            int n;
            while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
            bytes = buf.toByteArray();
        }
        final ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final int count = b.getInt();
        final List<Ray> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Ray r = new Ray();
            for (int k = 0; k < 3; k++) r.from[k] = b.getDouble();
            for (int k = 0; k < 3; k++) r.to[k] = b.getDouble();
            r.hit = b.get() != 0;
            for (int k = 0; k < 3; k++) r.where[k] = b.getDouble();
            out.add(r);
        }
        return out;
    }

    /** A context over the generated chunks x, z in 0..side-1, marked as if the game had given them. */
    static NetherPathfinder generatedWorld(int side) {
        final NetherPathfinder ctx = new NetherPathfinder(SEED, null, NetherPathfinder.Dimension.NETHER, 128);
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                ctx.getOrGenChunk(x, z);
                ctx.setChunkState(x, z, true);
            }
        }
        return ctx;
    }
}
