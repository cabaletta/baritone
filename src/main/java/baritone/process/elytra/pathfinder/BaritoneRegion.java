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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

/** Reads the chunks of one of Baritone's cached region files ({@code r.X.Z.bcr}). */
final class BaritoneRegion {

    private static final int MAGIC = 456022911;

    /** What the reader hands each chunk it finds. */
    interface ChunkSink {
        void accept(int chunkX, int chunkZ, Chunk chunk);
    }

    private BaritoneRegion() {}

    /**
     * The dimension of a Baritone cache directory such as {@code .../the_nether_128/regions},
     * or -1 if the directory is not one.
     */
    static int dimensionOf(String dir) {
        final Path parent = Paths.get(dir).getParent();
        final String name = parent == null || parent.getFileName() == null ? "" : parent.getFileName().toString();
        switch (name) {
            case "the_nether_128": return NetherPathfinder.DIMENSION_NETHER;
            case "overworld_384": return NetherPathfinder.DIMENSION_OVERWORLD;
            case "the_end_256": return NetherPathfinder.DIMENSION_END;
            default: return -1;
        }
    }

    static Path regionFile(String dir, int regionX, int regionZ) {
        return Paths.get(dir, "r." + regionX + "." + regionZ + ".bcr");
    }

    /** Reads the region file for (regionX, regionZ) under dir, if there is one. Returns whether a file was read. */
    static boolean load(String dir, int regionX, int regionZ, ChunkSink sink) {
        final int dim = dimensionOf(dir);
        if (dim == -1) {
            return false;
        }
        final Path file = regionFile(dir, regionX, regionZ);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file), 1 << 16)) {
            parse(in, regionX, regionZ, dim, sink);
        } catch (IOException | RuntimeException e) {
            System.err.println("[nether-pathfinder] exception thrown parsing region " + regionX + "," + regionZ + ": " + e);
        }
        return true;
    }

    /** Parses an already decompressed region stream. */
    static void parse(InputStream raw, int regionX, int regionZ, int dimension, ChunkSink sink) throws IOException {
        final DataInputStream in = new DataInputStream(raw);
        final int magic = in.readInt();
        if (magic != MAGIC) {
            System.err.println("[nether-pathfinder] bad magic for baritone region " + regionX + "," + regionZ);
            return;
        }
        final int height = dimension == NetherPathfinder.DIMENSION_OVERWORLD ? 384 : 256;
        final byte[] data = new byte[(2 * 16 * 16 * height) / 8];
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                final int present = in.readByte();
                if (present == 1) {
                    in.readFully(data);
                    sink.accept(x + 32 * regionX, z + 32 * regionZ, parseChunk(height, data));
                }
            }
        }
    }

    private static int positionIndex(int x, int y, int z) {
        return (x << 1) | (z << 5) | (y << 9);
    }

    private static int get2Bits(int i, byte[] data) {
        return (data[i / 8] >> (6 - (i % 8))) & 0b11;
    }

    private static Chunk parseChunk(int height, byte[] data) {
        final Chunk chunk = new Chunk();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (get2Bits(positionIndex(x, y, z), data) != 0) {
                        chunk.setBlock(x, y, z, true);
                    }
                }
            }
        }
        return chunk;
    }
}
