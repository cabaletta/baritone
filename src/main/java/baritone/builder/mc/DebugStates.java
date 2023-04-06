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

package baritone.builder.mc;

import baritone.builder.BlockData;
import baritone.builder.BlockStateCachedData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.*;
import java.util.stream.Stream;

public class DebugStates {

    public static void debug() {
        Map<Block, List<IBlockState>> bts = new HashMap<>();
        Map<String, List<IBlockState>> byString = new HashMap<>();
        BlockData data = new BlockData(new VanillaBlockStateDataProvider());
        for (IBlockState state : Block.BLOCK_STATE_IDS) {
            //System.out.println(state);
            bts.computeIfAbsent(state.getBlock(), $ -> new ArrayList<>()).add(state);
            String str = toString(data.get(Block.BLOCK_STATE_IDS.get(state)));
            byString.computeIfAbsent(str, $ -> new ArrayList<>()).add(state);
        }
        for (String key : (Iterable<String>) byString.keySet().stream().sorted()::iterator) {
            System.out.println("\n");
            System.out.println(key);
            Set<Block> skip = new HashSet<>();
            List<IBlockState> matches = byString.get(key);
            for (IBlockState state : matches) {
                if (skip.contains(state.getBlock())) {
                    continue;
                }
                if (matches.containsAll(bts.get(state.getBlock()))) {
                    System.out.println("All " + bts.get(state.getBlock()).size() + " variants of " + state.getBlock());
                    skip.add(state.getBlock());
                } else {
                    System.out.println(state);
                }
            }

        }
        /*System.out.println(Blocks.OAK_STAIRS.getDefaultState());
        System.out.println(Block.BLOCK_STATE_IDS.get(Blocks.OAK_STAIRS.getDefaultState()));
        System.out.println(Block.BLOCK_STATE_IDS.getByValue(Block.BLOCK_STATE_IDS.get(Blocks.OAK_STAIRS.getDefaultState())));*/
        Set<Block> normal = new HashSet<>();
        Block.REGISTRY.iterator().forEachRemaining(normal::add);
        Set<Block> alternate = new HashSet<>();
        for (IBlockState state : Block.BLOCK_STATE_IDS) {
            alternate.add(state.getBlock());
        }
        if (!alternate.equals(normal)) {
            throw new IllegalStateException();
        }
        outer:
        for (Block block : normal) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (Block.BLOCK_STATE_IDS.get(state) == -1) {
                    System.out.println(state + " doesn't exist?!");
                    continue;
                }
                if (block == Blocks.TRIPWIRE) {
                    System.out.println(state + " does exist");
                }
                if (!Block.BLOCK_STATE_IDS.getByValue(Block.BLOCK_STATE_IDS.get(state)).equals(state)) {
                    System.out.println(block + " is weird");
                    continue outer;
                }
            }
        }
    }

    public static String toString(BlockStateCachedData data) {
        if (data == null) {
            return "UNKNOWN";
        }
        Map<String, String> props = new LinkedHashMap<>();
        props(data, props);
        return props.toString();
    }

    private static void props(BlockStateCachedData data, Map<String, String> props) {
        if (data.isAir) {
            props.put("air", "true");
            return;
        }
        props.put("collides", "" + data.collidesWithPlayer);
        props.put("walkabletop", "" + data.fullyWalkableTop);
        props.put("placeme", "" + data.placeMe.size());
        props.put("sneak", "" + data.mustSneakWhenPlacingAgainstMe);
        props.put("againstme", "" + Stream.of(data.placeAgainstMe).filter(Objects::nonNull).count());
        if (data.collidesWithPlayer) {
            props.put("y", "" + data.collisionHeightBlips());
        }
    }
}
