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

package baritone.pathing.movement;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WaterloggedSolidTopTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static BlockState slab(SlabType type, boolean waterlogged) {
        return Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, type).setValue(BlockStateProperties.WATERLOGGED, waterlogged);
    }

    @Test
    public void waterloggedTopSlabIsSolidTop() {
        assertTrue(MovementHelper.isWater(slab(SlabType.TOP, true)));
        assertTrue(MovementHelper.isWaterloggedSolidTop(slab(SlabType.TOP, true)));
    }

    @Test
    public void otherBlocksAreNot() {
        assertFalse(MovementHelper.isWaterloggedSolidTop(slab(SlabType.TOP, false)));
        assertFalse(MovementHelper.isWaterloggedSolidTop(slab(SlabType.BOTTOM, true)));
        assertFalse(MovementHelper.isWaterloggedSolidTop(Blocks.WATER.defaultBlockState()));
        assertFalse(MovementHelper.isWaterloggedSolidTop(Blocks.STONE.defaultBlockState()));
    }
}
