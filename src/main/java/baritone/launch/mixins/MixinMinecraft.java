/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.bot.event.events.TickEvent;
import baritone.bot.event.events.WorldEvent;
import baritone.bot.event.events.type.EventState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author Brady
 * @since 7/31/2018 10:51 PM
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow private int leftClickCounter;
    @Shadow public WorldClient world;

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void init(CallbackInfo ci) {
        Baritone.INSTANCE.init();
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/client/Minecraft.currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
                    ordinal = 5,
                    shift = At.Shift.BY,
                    by = -3
            )
    )
    private void runTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Baritone.INSTANCE.getGameEventHandler().onTick(new TickEvent(
                EventState.PRE,
                (mc.player != null && mc.world != null)
                        ? TickEvent.Type.IN
                        : TickEvent.Type.OUT
        ));
    }

    @Redirect(
            method = "runTickKeyboard",
            at = @At(
                    value = "INVOKE",
                    target = "org/lwjgl/input/Keyboard.isKeyDown(I)Z"
            )
    )
    private boolean Keyboard$isKeyDown(int keyCode) {
        return Baritone.INSTANCE.getInputOverrideHandler().isKeyDown(keyCode);
    }

    @Inject(
            method = "processKeyBinds",
            at = @At("HEAD")
    )
    private void runTickKeyboard(CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onProcessKeyBinds();
    }

    @Redirect(
            method = {
                    "setIngameFocus",
                    "runTick"
            },
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "net/minecraft/client/Minecraft.leftClickCounter:I",
                    ordinal = 0
            )
    )
    private void setLeftClickCounter(Minecraft mc, int value) {
        if (!Baritone.INSTANCE.isInitialized() || !Baritone.INSTANCE.getInputOverrideHandler().isInputForcedDown(mc.gameSettings.keyBindAttack))
            this.leftClickCounter = value;
    }

    @Inject(
            method = "rightClickMouse",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "net/minecraft/client/entity/EntityPlayerSP.swingArm(Lnet/minecraft/util/EnumHand;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void postSwingArm(CallbackInfo ci, ItemStack stack, BlockPos pos, int stackCount, EnumActionResult result) {
        Minecraft mc = (Minecraft) (Object) this;
        Baritone bot = Baritone.INSTANCE;

        bot.getMemory().scanBlock(pos);
        bot.getMemory().scanBlock(pos.offset(mc.objectMouseOver.sideHit));
        bot.getActionHandler().onPlacedBlock(stack, pos);
    }

    @Inject(
            method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At("HEAD")
    )
    private void preLoadWorld(WorldClient world, String loadingMessage, CallbackInfo ci) {
        // If we're unloading the world but one doesn't exist, ignore it
        if (this.world == null && world == null)
            return;

        Baritone.INSTANCE.getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.PRE
                )
        );
    }

    @Inject(
            method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At("RETURN")
    )
    private void postLoadWorld(WorldClient world, String loadingMessage, CallbackInfo ci) {
        // If we're unloading the world but one doesn't exist, ignore it
        if (this.world == null && world == null)
            return;

        Baritone.INSTANCE.getGameEventHandler().onWorldEvent(
                new WorldEvent(
                        world,
                        EventState.POST
                )
        );
    }
}
