package baritone.awareness.model;

/** Snapshot of the local player's survival-relevant state, rebuilt every tick. */
public final class SelfState {
    public float health;
    public float maxHealth;
    public int   foodLevel;
    /** Average fraction of armor durability remaining across equipped slots (0–1). */
    public float totalArmorDurability;
    public boolean hasTotem;
    public int    totemCount;
    public boolean hasPearl;
    public int    pearlCount;
    public boolean hasGapple;
    public boolean hasPotion;
    public boolean shieldEquipped;
    /** Attack cooldown progress 0 (not ready) to 1 (fully charged). */
    public float attackCooldown;
    /** health + 20 if a totem is present, representing total HP buffer. */
    public float effectiveHp;
    /** True when our shield was disabled by an enemy axe hit (5-second cooldown). */
    public boolean shieldOnCooldown;
    /** 0–1 fraction of shield disable cooldown remaining (0 = ready, 1 = just broken). */
    public float shieldCooldownPercent;
}
