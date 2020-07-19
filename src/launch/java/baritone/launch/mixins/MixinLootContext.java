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

import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootPredicateManager;
import net.minecraft.loot.LootTableManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LootContext.Builder.class)
public class MixinLootContext {

    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/server/ServerWorld.getServer()Lnet/minecraft/server/MinecraftServer;"
            )
    )
    private MinecraftServer getServer(ServerWorld world) {
        if (world == null) {
            return null;
        }
        return world.getServer();
    }

    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/server/MinecraftServer.getLootTableManager()Lnet/minecraft/loot/LootTableManager;"
            )
    )
    private LootTableManager getLootTableManager(MinecraftServer server) {
        if (server == null) {
            return BlockOptionalMeta.getManager();
        }
        return server.getLootTableManager();
    }

    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/server/MinecraftServer.func_229736_aP_()Lnet/minecraft/loot/LootPredicateManager;"
            )
    )
    private LootPredicateManager getLootPredicateManager(MinecraftServer server) {
        if (server == null) {
            return BlockOptionalMeta.getPredicateManager();
        }
        return server.func_229736_aP_();
    }
}
