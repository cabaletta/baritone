package baritone.bot;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.IGameEventListener;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.ChunkEvent;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener {

    GameEventHandler() {
    }

    @Override
    public final void onTick() {
        dispatch(behavior -> onTick());
    }

    @Override
    public void onProcessKeyBinds() {
        InputOverrideHandler inputHandler = Baritone.INSTANCE.getInputOverrideHandler();

        // Simulate the key being held down this tick
        for (InputOverrideHandler.Input input : InputOverrideHandler.Input.values()) {
            KeyBinding keyBinding = input.getKeyBinding();

            if (inputHandler.isInputForcedDown(keyBinding) && !keyBinding.isKeyDown()) {
                int keyCode = keyBinding.getKeyCode();

                if (keyCode < Keyboard.KEYBOARD_SIZE)
                    KeyBinding.onTick(keyCode < 0 ? keyCode + 100 : keyCode);
            }
        }

        dispatch(behavior -> onProcessKeyBinds());
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        dispatch(behavior -> onSendChatMessage(event));
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        dispatch(behavior -> onChunkEvent(event));
    }

    private void dispatch(Consumer<Behavior> dispatchFunction) {
        Baritone.INSTANCE.getBehaviors().stream().filter(Behavior::isEnabled).forEach(dispatchFunction);
    }
}
