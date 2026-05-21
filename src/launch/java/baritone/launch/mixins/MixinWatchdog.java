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

import baritone.combat.watchdog.WatchdogEngine;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires WatchdogEngine.onTickEnd() at the tail of every Minecraft.tick().
 *
 * This is functionally equivalent to Fabric API's ClientTickEvents.END_CLIENT_TICK
 * without requiring the Fabric API dependency. The TAIL injection point guarantees:
 *   - All entity movement and input processing for this tick is complete
 *   - Player position, velocity, and health reflect the final state for this tick
 *   - The Watchdog sees the most up-to-date world snapshot before the next frame
 */
@Mixin(Minecraft.class)
public class MixinWatchdog {

    @Inject(method = "tick", at = @At("TAIL"))
    private void watchdogEndTick(CallbackInfo ci) {
        WatchdogEngine.onTickEnd(Minecraft.getInstance());
    }
}
