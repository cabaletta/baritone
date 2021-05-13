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

import baritone.builder.*;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * I expect this class to get extremely complicated.
 * <p>
 * Thankfully, all of it will be confined to this class, so when there's changes to new versions of Minecraft, e.g. new blocks, there will be only one place to look.
 */
public class BlockStatePropertiesExtractor {

    public static BlockStateCachedDataBuilder getData(IBlockState state) {
        Block block = state.getBlock();
        BlockStateCachedDataBuilder builder = new BlockStateCachedDataBuilder();

        // special cases
        {
            if (block instanceof BlockAir) {
                return builder.setAir();
            }
            if (block instanceof BlockStairs) {
                boolean rightsideUp = state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM; // true if normal stair, false if upside down stair
                Face facing = Face.fromMC(state.getValue(BlockStairs.FACING));
                BlockStateCachedDataBuilder stairBuilder = new BlockStateCachedDataBuilder() {
                    @Override
                    protected PlaceAgainstData placeAgainstFace(Face face) {
                        if (face == facing) {
                            // this is "the back" of the stair, which is a full face that you can place against just fine
                            return new PlaceAgainstData(face, Half.EITHER, isMustSneakWhenPlacingAgainstMe());
                        }
                        return super.placeAgainstFace(face);
                    }
                };
                if (!rightsideUp) {
                    stairBuilder.fullyWalkableTop();
                    stairBuilder.height(1);
                }
                return stairBuilder.mustBePlacedAgainst(rightsideUp ? Half.BOTTOM : Half.TOP)
                        .collidesWithPlayer(true)
                        .canPlaceAgainstMe()
                        .playerMustBeFacingInOrderToPlaceMe(facing);
            }
            if (block instanceof BlockSlab) {
                double height;
                Half mustBePlacedAgainst;
                if (((BlockSlab) block).isDouble()) {
                    height = 1;
                    mustBePlacedAgainst = Half.EITHER;
                } else if (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM) {
                    height = 0.5;
                    mustBePlacedAgainst = Half.BOTTOM;
                } else {
                    height = 1;
                    mustBePlacedAgainst = Half.TOP;
                }
                return builder
                        .mustBePlacedAgainst(mustBePlacedAgainst)
                        .fullyWalkableTop()
                        .height(height)
                        .canPlaceAgainstMe()
                        .collidesWithPlayer(true);
            }
            if (block instanceof BlockTrapDoor) {
                boolean bottom = state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.BOTTOM;
                Face facing = Face.fromMC(state.getValue(BlockTrapDoor.FACING));
                return new BlockStateCachedDataBuilder() {
                    @Override
                    public List<BlockStatePlacementOption> howCanIBePlaced() {
                        List<BlockStatePlacementOption> ret = new ArrayList<>();
                        if (!(Main.STRICT_Y && !bottom)) {
                            ret.add(BlockStatePlacementOption.get(bottom ? Face.DOWN : Face.UP, Half.EITHER, Optional.ofNullable(facing.opposite())));
                        }
                        ret.add(BlockStatePlacementOption.get(facing.opposite(), bottom ? Half.BOTTOM : Half.TOP, Optional.empty()));
                        return Collections.unmodifiableList(ret);
                    }
                }.collidesWithPlayer(true); // TODO allow walking on top of closed top-half trapdoor? lol
            }
        }

        {
            // blocks that have to be placed with the player facing a certain way

            // rotated clockwise about the Y axis
            if (block instanceof BlockAnvil) {
                builder.playerMustBeFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockAnvil.FACING).rotateYCCW()));
            }

            // unchanged
            if (block instanceof BlockChest
                // it is not right || block instanceof BlockSkull // TODO is this right?? skull can be any facing?
            ) { // TODO fence gate and lever
                builder.playerMustBeFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockHorizontal.FACING)));
            }

            // opposite
            if (block instanceof BlockCocoa
                    || block instanceof BlockEnderChest
                    || block instanceof BlockEndPortalFrame
                    || block instanceof BlockFurnace
                    || block instanceof BlockGlazedTerracotta
                    || block instanceof BlockPumpkin
                    || block instanceof BlockRedstoneDiode // both repeater and comparator
            ) {
                builder.playerMustBeFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockHorizontal.FACING)).opposite());
            }
        }
        // getStateForPlacement.against is the against face. placing a torch will have it as UP. placing a bottom slab will have it as UP. placing a top slab will have it as DOWN.
        // ladder

        // fully passable blocks
        {
            // some ways of determining this list that don't work:
            // calling isPassable: even though blocks such as carpet and redstone repeaters are marked as passable, they are not really, they do have a height
            // checking material: Material.PLANTS is not enough because it includes cocoa and chorus, Material.CIRCUITS is not enough because it includes redstone repeaters

            // these are the blocks that you can fully walk through with no collision at all, such as torches and sugar cane
            if (block instanceof BlockBush // includes crops
                    || block instanceof BlockReed
                    || block instanceof BlockTorch
                    || (block instanceof BlockSnow && state.getValue(BlockSnow.LAYERS) == 1)
                    //|| block instanceof BlockSign
                    || block instanceof BlockRedstoneWire
                    || block instanceof BlockRailBase
                    || (block instanceof BlockFenceGate && state.getValue(BlockFenceGate.OPEN))
                    || block instanceof BlockLever
                    || block instanceof BlockButton
                    || block instanceof BlockBanner

                // TODO include pressure plate, tripwire, tripwire hook?
            ) {
                builder.collidesWithPlayer(false);
            } else {
                builder.collidesWithPlayer(true);
            }
        }

        // TODO getDirectionFromEntityLiving

        // TODO multiblocks like door and bed and double plant


        boolean fullyUnderstood = false; // set this flag to true for any state for which we have fully and completely described it


        if (state.isBlockNormalCube() || state.isFullBlock() || block instanceof BlockGlass || block instanceof BlockStainedGlass) {
            builder.canPlaceAgainstMe();
            fullyUnderstood = true;
        }

        if (state.isBlockNormalCube()) {
            builder.fullyWalkableTop().height(1);
            fullyUnderstood = true;
        }

        if (block instanceof BlockSnow) {
            fullyUnderstood = true;
            if (state.getValue(BlockSnow.LAYERS) > 1) { // collidesWithPlayer false from earlier
                builder.fullyWalkableTop().height(0.125 * (state.getValue(BlockSnow.LAYERS) - 1));
                // funny - if you have snow layers packed 8 high, it only supports the player to a height of 0.875, but it still counts as "isTopSolid" for placing stuff like torches on it
            }
        }

        if (block instanceof BlockSoulSand) {
            builder.height(0.875);
        }


        // TODO fully walkable top and height

        return builder;
    }
}
