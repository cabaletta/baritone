package baritone.bot.event;

import net.minecraft.client.Minecraft;

/**
 * @author Brady
 * @since 7/31/2018 11:05 PM
 */
public interface IGameEventListener {

    /**
     * Run once per game tick from {@link Minecraft#runTick}.
     */
    void onTick();
}
