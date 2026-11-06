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

package baritone.api.utils;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockOptionalMetaLookupTest {

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void mineLookupPreservesSelectedStatesInBothDropModes() {
        BlockOptionalMeta sourceWater = new BlockOptionalMeta("water[level=0]");
        BlockState source = Blocks.WATER.defaultBlockState();
        BlockState flowing = source.setValue(LiquidBlock.LEVEL, 1);

        for (boolean silkTouch : new boolean[] {false, true}) {
            BlockOptionalMetaLookup lookup = new BlockOptionalMetaLookup(List.of(sourceWater), silkTouch);
            assertTrue(lookup.has(source));
            assertFalse(lookup.has(flowing));
            assertFalse(lookup.blocks().get(0).matches(flowing));
        }
    }
}
