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
import baritone.utils.accessor.IImmersivePortalsClientChunkProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Pseudo
@Mixin(targets = "qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager", remap = false) //bruh
public class MixinImmersivePortalsChunkProvider extends ClientChunkCache implements IImmersivePortalsClientChunkProvider {
    
    @Shadow(aliases = {"chunkMapNew"})
    @Final
    @Mutable
    protected Long2ObjectLinkedOpenHashMap<LevelChunk> chunkMap;
    
    @Unique
    private static Constructor<?> thisConstructor;
    
    //protected in 1.16
    @Shadow
    @Final
    protected ClientLevel world;
    
    
    @Override
    public ClientChunkCache createThreadSafeCopy() {
        try {
            if (thisConstructor == null) {
                thisConstructor = this.getClass().getConstructor(ClientLevel.class, int.class);
            }
            IImmersivePortalsClientChunkProvider result = (IImmersivePortalsClientChunkProvider) thisConstructor.newInstance(world, 0); // distance literally doesn't matter because of how they do it lmao
            result.overwriteChunkMap(new Long2ObjectLinkedOpenHashMap<>(chunkMap));
            return (ClientChunkCache) result;
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Immersive Portals chunk manager initialization for baritone failed", e);
        }
    }
    
    @Override
    public void overwriteChunkMap(Long2ObjectLinkedOpenHashMap<LevelChunk> chunks) {
        chunkMap = chunks;
    }
    
    @Override
    public IChunkArray extractReferenceArray() {
        return null;
    }
    
    public MixinImmersivePortalsChunkProvider(ClientLevel clientWorldIn, int viewDistance) {
        super(clientWorldIn, viewDistance);
    }
}
