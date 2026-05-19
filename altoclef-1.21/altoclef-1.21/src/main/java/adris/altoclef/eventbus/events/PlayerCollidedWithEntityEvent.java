package adris.altoclef.eventbus.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerCollidedWithEntityEvent {
    public PlayerEntity player;
    public Entity other;

    public PlayerCollidedWithEntityEvent(PlayerEntity player, Entity other) {
        this.player = player;
        this.other = other;
    }
}
