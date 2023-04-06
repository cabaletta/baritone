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

package baritone.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * There will be a BlockStateCachedData for every IBlockState in the game, yes.
 * <p>
 * But, we need some additional BlockStateCachedDatas. For example, we need one that represents hypothetical scaffolding, and another for air, for properly computing place order dependency graphs. Some other places need a placeholder for out of bounds.
 * <p>
 * We could just say that scaffolding is always, like, dirt, or something. But that would be inelegant.
 * <p>
 * And beyond the needs at runtime, unit tests shouldn't need to bootstrap and boot up the entire Minecraft game. So, let's have some fake BlockStateCachedData for testing purposes too!
 */
public class FakeStates {
    // the three aformentioned placeholders for runtime
    public static final BlockStateCachedData SCAFFOLDING = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().collisionHeight(1).canPlaceAgainstMe());
    // need a second solid block so that "== FakeStates.SCAFFOLDING" doesn't get tricked
    public static final BlockStateCachedData SOLID = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().collisionHeight(1).canPlaceAgainstMe());
    public static final BlockStateCachedData AIR = new BlockStateCachedData(new BlockStateCachedDataBuilder().setAir());
    public static final BlockStateCachedData OUT_OF_BOUNDS = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).collisionHeight(1));

    // and some for testing
    public static final BlockStateCachedData[] BY_HEIGHT;

    static {
        BY_HEIGHT = new BlockStateCachedData[Blip.FULL_BLOCK + 1];
        for (int height = 1; height <= Blip.FULL_BLOCK; height++) {
            BY_HEIGHT[height] = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().collisionHeight(height * Blip.RATIO));
        }
        BY_HEIGHT[0] = AIR;
    }

    private static List<BlockStateCachedData> PROBABLE_BLOCKS = null;

    private static List<BlockStateCachedData> getProbableBlocks() {
        if (PROBABLE_BLOCKS == null) {
            //long a = System.currentTimeMillis();
            Random rand = new Random(5021);
            PROBABLE_BLOCKS = IntStream.range(0, 10000).mapToObj($ -> {
                List<BlockStatePlacementOption> ret = new ArrayList<>(SCAFFOLDING.placeMe);
                ret.removeIf($$ -> rand.nextInt(10) < 2);
                BlockStateCachedDataBuilder builder = new BlockStateCachedDataBuilder() {
                    @Override
                    public List<BlockStatePlacementOption> howCanIBePlaced() {
                        return ret;
                    }
                };
                if (ret.isEmpty()) {
                    builder.placementLogicNotImplementedYet();
                }
                return new BlockStateCachedData(builder
                        .fullyWalkableTop()
                        .collisionHeight(1)
                        .canPlaceAgainstMe()
                        .collidesWithPlayer(true));
            }).collect(Collectors.toList());
            //System.out.println("Took " + (System.currentTimeMillis() - a));
        }
        return PROBABLE_BLOCKS;
    }

    public static BlockStateCachedData probablyCanBePlaced(Random rand) {
        return getProbableBlocks().get(rand.nextInt(getProbableBlocks().size()));
    }

    // probably more will go here such as for making sure that slabs and stairs work right (like there'll be a fake slab and a fake stair i guess?)
}
