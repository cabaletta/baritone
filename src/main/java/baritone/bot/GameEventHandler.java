package baritone.bot;

import baritone.bot.event.IGameEventListener;
import baritone.Baritone;

/**
 * @author Brady
 * @since 7/31/2018 11:04 PM
 */
public final class GameEventHandler implements IGameEventListener {

    GameEventHandler() {}

    @Override
    public final void onTick() {
    	Baritone.onTick();
    }
}
