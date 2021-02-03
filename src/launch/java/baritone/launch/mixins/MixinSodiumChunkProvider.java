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
import baritone.utils.accessor.IClientChunkProvider;
import baritone.utils.accessor.ISodiumChunkArray;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.SodiumChunkManager", remap = false)
public class MixinSodiumChunkProvider implements IClientChunkProvider {
    
    @Shadow
    @Final
    private StampedLock lock;
    
    @Shadow
    @Final
    private ClientWorld world;
    
    @Shadow
    private int radius;
    
    @Unique
    private static Constructor<?> thisConstructor = null;
    
    @Unique
    private static Field chunkArrayField = null;
    
    @Override
    public ClientChunkProvider createThreadSafeCopy() {
        // similar operation to https://github.com/jellysquid3/sodium-fabric/blob/d3528521d48a130322c910c6f0725cf365ebae6f/src/main/java/me/jellysquid/mods/sodium/client/world/SodiumChunkManager.java#L139
        long stamp = this.lock.writeLock();
        
        try {
            ISodiumChunkArray refArray = extractReferenceArray();
            if (thisConstructor == null) {
                thisConstructor = this.getClass().getConstructor(ClientWorld.class, int.class);
            }
            ClientChunkProvider result = (ClientChunkProvider) thisConstructor.newInstance(world, radius - 3); // -3 because it adds 3 for no reason here too lmao
            IChunkArray copyArr = ((IClientChunkProvider) result).extractReferenceArray();
            copyArr.copyFrom(refArray);
            return result;
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Sodium chunk manager initialization for baritone failed", e);
        } finally {
            // put this in finally so we can't break anything.
            this.lock.unlockWrite(stamp);
        }
    }
    
    @Override
    public ISodiumChunkArray extractReferenceArray() {
        https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.15
        if (chunkArrayField == null) {
            for (Field f : this.getClass().getDeclaredFields()) {
                if (ISodiumChunkArray.class.isAssignableFrom(f.getType())) {
                    chunkArrayField = f;
                    break https;
                }
            }
            throw new RuntimeException(Arrays.toString(this.getClass().getDeclaredFields()));
        }
        try {
            return (ISodiumChunkArray) chunkArrayField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
