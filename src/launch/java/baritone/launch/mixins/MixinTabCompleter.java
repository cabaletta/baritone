package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.TabCompleteEvent;
import baritone.utils.accessor.ITabCompleter;
import net.minecraft.client.Minecraft;
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

    @Inject(method = "requestCompletions", at = @At("HEAD"), cancellable = true)
    private void onRequestCompletions(String prefix, CallbackInfo ci) {
        if (!isChatCompleter) {
            return;
        }

        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(Minecraft.getMinecraft().player);

        if (isNull(baritone)) {
            return;
        }

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
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(Minecraft.getMinecraft().player);

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
