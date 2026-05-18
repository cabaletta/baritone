package baritone.awareness.model;

import java.util.UUID;

/** Combat profile for a tracked enemy player, updated by CombatStatSensor. */
public final class CombatStats {
    public UUID entityId;
    /** Total armor value (0-20). */
    public float armorPoints;
    public boolean shieldEquipped;
    public boolean isBlocking;
    /** Estimated damage per swing, accounting for attack cooldown. */
    public float estimatedDps;
    public int totemPops;
    /** Attack cooldown 0-1. */
    public float attackCooldown;
    public boolean holdingCrystal;
    public boolean holdingTotem;
}
