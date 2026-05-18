package baritone.awareness.sensor;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.CombatStats;
import baritone.awareness.model.EntityCategory;
import baritone.awareness.model.TrackedEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads armor value, shield state, and estimated DPS for each tracked enemy player. */
public final class CombatStatSensor {

    private final IPlayerContext ctx;
    private final Map<UUID, CombatStats> statsMap = new HashMap<>();

    public CombatStatSensor(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void update(List<TrackedEntity> entities) {
        Set<UUID> activeIds = new HashSet<>();

        for (TrackedEntity te : entities) {
            if (te.category != EntityCategory.ENEMY_PLAYER) continue;
            if (!(te.entity instanceof Player)) continue;
            Player player = (Player) te.entity;
            UUID id = player.getUUID();
            activeIds.add(id);

            CombatStats stats = statsMap.computeIfAbsent(id, k -> new CombatStats());
            stats.entityId = id;
            stats.armorPoints = player.getArmorValue();
            stats.isBlocking = player.isBlocking();
            stats.shieldEquipped = player.getOffhandItem().getItem() == Items.SHIELD
                                || player.getMainHandItem().getItem() == Items.SHIELD;
            stats.attackCooldown = player.getAttackStrengthScale(0f);
            stats.holdingCrystal = player.getMainHandItem().getItem() == Items.END_CRYSTAL
                                || player.getOffhandItem().getItem() == Items.END_CRYSTAL;
            stats.holdingTotem = player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING
                              || player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
            // getAttributeValue already factors in weapon bonus from ATTACK_DAMAGE attribute
            stats.estimatedDps = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                                         * stats.attackCooldown);
        }

        statsMap.keySet().retainAll(activeIds);
    }

    public CombatStats getStats(UUID id) {
        return statsMap.get(id);
    }

    public Map<UUID, CombatStats> getAllStats() {
        return statsMap;
    }
}
