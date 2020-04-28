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

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RenderEvent;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(
            method = "renderWorldPass",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
                    args = {"ldc=hand"}
            )
    )
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        int renderViewDimension = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().provider.getDimensionType().getId();

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            World world = ibaritone.getPlayerContext().world();
            if (world == null || world.provider.getDimensionType().getId() != renderViewDimension) {
                continue;
            }
            ibaritone.getGameEventHandler().onRenderPass(new RenderEvent(partialTicks));
        }
    }
}
