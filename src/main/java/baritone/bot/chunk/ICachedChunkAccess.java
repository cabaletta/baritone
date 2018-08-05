package baritone.bot.chunk;

import baritone.bot.pathing.util.IBlockTypeAccess;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/4/2018 1:10 AM
 */
public interface ICachedChunkAccess extends IBlockTypeAccess {

    void updateCachedChunk(int chunkX, int chunkZ, BitSet data);
}
