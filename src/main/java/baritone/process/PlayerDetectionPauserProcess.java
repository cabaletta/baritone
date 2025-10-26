package baritone.process;

import baritone.Baritone;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameType;

/*
 * Pauses when players are nearby or goes into spectator mode
 * 
 * @author Grayson-code 
 */
public class PlayerDetectionPauserProcess extends BaritoneProcessHelper {

    private boolean shouldPauseForSpectator = false;
    private boolean shouldPauseForNearbyPlayer = false;

    public PlayerDetectionPauserProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        if (ctx.player() == null || ctx.world() == null) {
            return false;
        }
        
        return Baritone.settings().pauseOnSpectator.value || Baritone.settings().pauseOnPlayerNearby.value;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        shouldPauseForSpectator = false;
        shouldPauseForNearbyPlayer = false;

        if (Baritone.settings().pauseOnSpectator.value) {
            shouldPauseForSpectator = checkForSpectatorPlayers();
        }

        if (Baritone.settings().pauseOnPlayerNearby.value) {
            shouldPauseForNearbyPlayer = checkForNearbyPlayers();
        }

        if (shouldPauseForSpectator || shouldPauseForNearbyPlayer) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    /**
     * Check if any player on the server is in spectator mode
     * @return true if at least one player is in spectator mode
     */
    private boolean checkForSpectatorPlayers() {
        try {
            for (Entity entity : ctx.entities()) {
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    
                    if (player.getUniqueID().equals(ctx.player().getUniqueID())) {
                        continue;
                    }
                    
                    if (player.isSpectator()) {
                        logDirect("Paused: Player in spectator mode detected - " + player.getName().getString());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Check if any player is within the configured radius
     * @return true if at least one player is nearby
     */
    private boolean checkForNearbyPlayers() {
        try {
            double radiusSquared = Math.pow(Baritone.settings().pauseOnPlayerNearbyRadius.value, 2);
            
            for (Entity entity : ctx.entities()) {
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    
                    // Skip the local player
                    if (player.getUniqueID().equals(ctx.player().getUniqueID())) {
                        continue;
                    }
                    
                    // Check distance to player
                    double distanceSquared = ctx.player().getDistanceSq(player);
                    if (distanceSquared <= radiusSquared) {
                        logDirect("Paused: Player detected nearby - " + player.getName().getString() + 
                                " (distance: " + String.format("%.1f", Math.sqrt(distanceSquared)) + " blocks)");
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public void onLostControl() {
        // Reset pause flags when losing control
        shouldPauseForSpectator = false;
        shouldPauseForNearbyPlayer = false;
    }

    @Override
    public String displayName0() {
        return "Player Detection Pauser";
    }

    @Override
    public double priority() {
        // Higher priority than inventory pauser (5.1) to ensure we pause first
        return 5.2;
    }

    @Override
    public boolean isTemporary() {
        return true;
    }
}
