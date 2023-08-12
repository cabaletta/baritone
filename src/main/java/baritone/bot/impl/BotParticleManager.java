/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.impl;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 3/8/2020
 */
public final class BotParticleManager extends ParticleManager {

    public static final BotParticleManager INSTANCE = new BotParticleManager();

    private BotParticleManager() {
        super(null, null);
    }

    @Override
    public void emitParticleAtEntity(Entity entityIn, @Nonnull EnumParticleTypes particleTypes) {}

    @Override
    public void emitParticleAtEntity(Entity p_191271_1_, @Nonnull EnumParticleTypes p_191271_2_, int p_191271_3_) {}
}
