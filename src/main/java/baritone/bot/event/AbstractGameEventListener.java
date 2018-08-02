package baritone.bot.event;

import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.ChunkEvent;

/**
 * An implementation of {@link IGameEventListener} that has all methods
 * overriden with empty bodies, allowing inheritors of this class to choose
 * which events they would like to listen in on.
 *
 * Has no implementors at the moment, but will likely be used with the
 * manager/behavior system is ported over to the new system.
 *
 * @see IGameEventListener
 *
 * @author Brady
 * @since 8/1/2018 6:29 PM
 */
public interface AbstractGameEventListener extends IGameEventListener {

    @Override
    default void onTick() {}

    @Override
    default void onProcessKeyBinds() {}

    @Override
    default void onSendChatMessage(ChatEvent event) {}

    @Override
    default void onChunkEvent(ChunkEvent event) {}
}
