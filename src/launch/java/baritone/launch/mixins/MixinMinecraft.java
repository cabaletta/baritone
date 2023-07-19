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
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

/**
 * @author Brady
 * @since 7/31/2018
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    public LocalPlayer player;
    @Shadow
    public ClientLevel level;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void postInit(CallbackInfo ci) {
        BaritoneAPI.getProvider().getPrimaryBaritone();
    }


    @Inject(
            method = "tick",
            at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
            ordinal = 4,
            shift  = At.Shift.BY,
            by = -3
            )
    )
    private void runTick(CallbackInfo ci) {
        final BiFunction<EventState, TickEvent.Type, TickEvent> tickProvider = TickEvent.createNextProvider();

        for (IBaritone baritone : BaritoneAPI.getProvider().getAllBaritones()) {

            TickEvent.Type type = baritone.getPlayerContext().player() != null && baritone.getPlayerContext().world() != null
                    ? TickEvent.Type.IN
                    : TickEvent.Type.OUT;

            baritone.getGameEventHandler().onTick(tickProvider.apply(EventState.PRE, type));
        }
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/multiplayer/ClientLevel.tickEntities()V",
                    shift = At.Shift.AFTER
            )
    )
    private void postUpdateEntities(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(this.player);
        if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
        }
    }

    @Inject(
            method = "setLevel",
            at = @At("HEAD")
    )
    private void preLoadWorld(ClientLevel world, CallbackInfo ci) {
        // If we're unloading the world but one doesn't exist, ignore it
        if (this.level == null && world == null) {
            return;
        }

        // mc.world changing is only the primary baritone

        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.PRE
                )
        );
    }

    @Inject(
            method = "setLevel",
            at = @At("RETURN")
    )
    private void postLoadWorld(ClientLevel world, CallbackInfo ci) {
        // still fire event for both null, as that means we've just finished exiting a world

        // mc.world changing is only the primary baritone
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.POST
                )
        );
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/gui/screens/Screen;passEvents:Z"
            )
    )
    private boolean passEvents(Screen screen) {
        // allow user input is only the primary baritone
        return (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && player != null) || screen.passEvents;
    }

    // TODO
    // FIXME
    // bradyfix
    // i cant mixin
    // lol
    // https://discordapp.com/channels/208753003996512258/503692253881958400/674760939681349652
    // https://discordapp.com/channels/208753003996512258/503692253881958400/674756457966862376
    /*@Inject(
            method = "rightClickMouse",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/entity/player/ClientPlayerEntity.swingArm(Lnet/minecraft/util/Hand;)V",
                    ordinal = 1
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onBlockUse(CallbackInfo ci, Hand var1[], int var2, int var3, Hand enumhand, ItemStack itemstack, EntityRayTraceResult rt, Entity ent, ActionResultType art, BlockRayTraceResult raytrace, int i, ActionResultType enumactionresult) {
        // rightClickMouse is only for the main player
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onBlockInteract(new BlockInteractEvent(raytrace.getPos(), BlockInteractEvent.Type.USE));
    }*/
}
