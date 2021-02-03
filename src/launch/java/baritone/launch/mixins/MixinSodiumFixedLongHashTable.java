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

package baritone.launch.mixins;

import baritone.utils.accessor.IChunkArray;
import baritone.utils.accessor.ISodiumChunkArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.util.collections.FixedLongHashTable", remap = false)
public abstract class MixinSodiumFixedLongHashTable implements ISodiumChunkArray {
    
    @Shadow
    public abstract ObjectIterator<Long2ObjectMap.Entry<Object>> iterator();
    
    @Shadow
    public abstract Object put(final long k, final Object v);
    
    /**
    * similar to https://github.com/jellysquid3/sodium-fabric/blob/d3528521d48a130322c910c6f0725cf365ebae6f/src/main/java/me/jellysquid/mods/sodium/client/world/SodiumChunkManager.java#L149
    * except that since we aren't changing the radius, the key value doesn't change.
    */
    @Override
    public void copyFrom(IChunkArray other) {
        ObjectIterator<Long2ObjectMap.Entry<Object>> it = ((ISodiumChunkArray) other).callIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<Object> entry = it.next();
            this.put(entry.getLongKey(), entry.getValue());
        }
    }
    
    @Override
    public ObjectIterator<Long2ObjectMap.Entry<Object>> callIterator() {
        return iterator();
    }
    
    //these are useless here...
    @Override
    public AtomicReferenceArray<Chunk> getChunks() {
        return null;
    }
    
    @Override
    public int centerX() {
        return 0;
    }
    
    @Override
    public int centerZ() {
        return 0;
    }
    
    @Override
    public int viewDistance() {
        return 0;
    }
    
}
