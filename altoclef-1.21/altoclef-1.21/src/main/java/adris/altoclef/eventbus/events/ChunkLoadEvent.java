package adris.altoclef.eventbus.events;

import net.minecraft.world.chunk.WorldChunk;

public class ChunkLoadEvent {
    public WorldChunk chunk;

    public ChunkLoadEvent(WorldChunk chunk) {
        this.chunk = chunk;
    }
}
