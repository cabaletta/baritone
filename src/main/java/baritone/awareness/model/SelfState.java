package baritone.awareness.model;

/** Snapshot of the local player's survival-relevant state, rebuilt every tick. */
public final class SelfState {
    public float health;
    public float maxHealth;
    public int foodLevel;
    /** Average fraction of armor durability remaining across equipped slots (0-1). */
    public float totalArmorDurability;
    public boolean hasTotem;
    public int totemCount;
    public boolean hasPearl;
    public int pearlCount;
    public boolean hasGapple;
    public boolean hasPotion;
    public boolean shieldEquipped;
    /** Attack cooldown progress 0 (not ready) to 1 (fully charged). */
    public float attackCooldown;
    /** health + 20 if a totem is present, representing the total HP buffer. */
    public float effectiveHp;
}
