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
import baritone.api.event.events.TabCompleteEvent;
import baritone.utils.accessor.ITabCompleter;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static java.util.Objects.isNull;

@Mixin(TabCompleter.class)
public abstract class MixinTabCompleter implements ITabCompleter {
    @Shadow
    @Final
    protected GuiTextField textField;

    @Shadow
    protected boolean requestedCompletions;

    @Shadow
    public abstract void setCompletions(String... newCompl);

    @Unique
    protected boolean isChatCompleter = false;

    @Unique
    protected boolean dontComplete = false;

    @Override
    public String getPrefix() {
        return textField.getText().substring(0, textField.getCursorPosition());
    }

    @Override
    public void setPrefix(String prefix) {
        textField.setText(prefix + textField.getText().substring(textField.getCursorPosition()));
        textField.setCursorPosition(prefix.length());
    }

    @Inject(
            method = "requestCompletions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRequestCompletions(String prefix, CallbackInfo ci) {
        if (!isChatCompleter) {
            return;
        }

        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

        TabCompleteEvent.Pre event = new TabCompleteEvent.Pre(prefix);
        baritone.getGameEventHandler().onPreTabComplete(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.prefix.wasModified()) {
            setPrefix(event.prefix.get());
        }

        if (event.completions.wasModified()) {
            ci.cancel();

            dontComplete = true;

            try {
                requestedCompletions = true;
                setCompletions(event.completions.get());
            } finally {
                dontComplete = false;
            }
        }
    }

    @Override
    public boolean onGuiChatSetCompletions(String[] newCompl) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

        if (isNull(baritone)) {
            return false;
        }

        TabCompleteEvent.Post event = new TabCompleteEvent.Post(getPrefix(), newCompl);
        baritone.getGameEventHandler().onPostTabComplete(event);

        if (event.isCancelled()) {
            return true;
        }

        if (event.prefix.wasModified()) {
            String prefix = event.prefix.get();
            textField.setText(prefix + textField.getText().substring(textField.getCursorPosition()));
            textField.setCursorPosition(prefix.length());
        }

        if (event.completions.wasModified()) {
            setCompletions(event.completions.get());
            return true;
        }

        return false;
    }
}
