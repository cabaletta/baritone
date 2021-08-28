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
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
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

        // returns null only if we don't know how to walk on / walk around / place against this block
        // if we don't know how to place the block, get everything else but add .placementLogicNotImplementedYe

        // special cases
        {
            if (block instanceof BlockAir || block instanceof BlockStructureVoid) {
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
                }
                return stairBuilder.mustBePlacedAgainst(rightsideUp ? Half.BOTTOM : Half.TOP)
                        .collidesWithPlayer(true)
                        .collisionHeight(1)
                        .canPlaceAgainstMe()
                        .playerMustBeHorizontalFacingInOrderToPlaceMe(facing);
            }
            if (block instanceof BlockSlab) {
                if (((BlockSlab) block).isDouble()) {
                    builder.placementLogicNotImplementedYet().collisionHeight(1);
                } else if (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM) {
                    builder.mustBePlacedAgainst(Half.BOTTOM).collisionHeight(0.5);
                } else {
                    builder.mustBePlacedAgainst(Half.TOP).collisionHeight(1);
                }
                return builder
                        .fullyWalkableTop()
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
                            ret.add(BlockStatePlacementOption.get(bottom ? Face.DOWN : Face.UP, Half.EITHER, Optional.ofNullable(facing.opposite()), Optional.empty()));
                        }
                        ret.add(BlockStatePlacementOption.get(facing.opposite(), bottom ? Half.BOTTOM : Half.TOP, Optional.empty(), Optional.empty()));
                        return ret;
                    }
                }
                        .collisionHeight(1) // sometimes it can be 1, and for collision height we err on the side of max
                        .collidesWithPlayer(true); // dont allow walking on top of closed top-half trapdoor because redstone activation is scary and im not gonna predict it
            }
            if (block instanceof BlockLog) {
                BlockLog.EnumAxis axis = state.getValue(BlockLog.LOG_AXIS);
                BlockStateCachedDataBuilder logBuilder = new BlockStateCachedDataBuilder() {
                    @Override
                    public List<BlockStatePlacementOption> howCanIBePlaced() {
                        List<BlockStatePlacementOption> ret = super.howCanIBePlaced();
                        ret.removeIf(place -> BlockLog.EnumAxis.fromFacingAxis(place.against.toMC().getAxis()) != axis);
                        return ret;
                    }
                };
                if (axis == BlockLog.EnumAxis.NONE) {
                    logBuilder.placementLogicNotImplementedYet(); // ugh
                }
                return logBuilder
                        .fullyWalkableTop()
                        .collisionHeight(1)
                        .canPlaceAgainstMe()
                        .collidesWithPlayer(true);
            }
            if (block instanceof BlockRotatedPillar) { // hay block, bone block
                // even though blocklog inherits from blockrotatedpillar it uses its own stupid enum, ugh
                // this is annoying because this is pretty much identical
                EnumFacing.Axis axis = state.getValue(BlockRotatedPillar.AXIS);
                BlockStateCachedDataBuilder rotatedPillarBuilder = new BlockStateCachedDataBuilder() {
                    @Override
                    public List<BlockStatePlacementOption> howCanIBePlaced() {
                        List<BlockStatePlacementOption> ret = super.howCanIBePlaced();
                        ret.removeIf(place -> place.against.toMC().getAxis() != axis);
                        return ret;
                    }
                };
                return rotatedPillarBuilder
                        .fullyWalkableTop()
                        .collisionHeight(1)
                        .canPlaceAgainstMe()
                        .collidesWithPlayer(true);
            }
            if (block instanceof BlockStructure) {
                return null; // truly an error to encounter this
            }
        }

        {
            // blocks that have to be placed with the player facing a certain way

            // rotated clockwise about the Y axis
            if (block instanceof BlockAnvil) {
                builder.playerMustBeHorizontalFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockAnvil.FACING).rotateYCCW()));
            }

            // unchanged
            if (block instanceof BlockChest
                // it is not right || block instanceof BlockSkull // TODO is this right?? skull can be any facing?
            ) { // TODO fence gate and lever
                builder.playerMustBeHorizontalFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockHorizontal.FACING)));
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
                builder.playerMustBeHorizontalFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockHorizontal.FACING)).opposite());
            }
        }
        // ladder

        {
            if (block instanceof BlockContainer || block instanceof BlockWorkbench) {
                // TODO way more blocks have a right click action, e.g. redstone repeater, daylight sensor
                builder.mustSneakWhenPlacingAgainstMe();
            }
        }

        if (block instanceof BlockCommandBlock
                || block instanceof BlockDispenser // dropper extends from dispenser
                || block instanceof BlockPistonBase) {
            builder.playerMustBeEntityFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockDirectional.FACING)));
        }

        if (block instanceof BlockObserver) {
            builder.playerMustBeEntityFacingInOrderToPlaceMe(Face.fromMC(state.getValue(BlockDirectional.FACING)).opposite());
        }

        // fully passable blocks
        {
            // some ways of determining this list that don't work:
            // calling isPassable: even though blocks such as carpet and redstone repeaters are marked as passable, they are not really, they do have a height
            // checking material: Material.PLANTS is not enough because it includes cocoa and chorus, Material.CIRCUITS is not enough because it includes redstone repeaters

            // these are the blocks that you can fully walk through with no collision at all, such as torches and sugar cane
            if (block instanceof BlockBush // includes crops
                    || block instanceof BlockReed
                    || block instanceof BlockTorch
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

        if (block instanceof BlockFalling) {
            builder.mustBePlacedBottomToTop();
        }


        // TODO multiblocks like door and bed and double plant


        boolean fullyUnderstood = false; // set this flag to true for any state for which we have fully and completely described it

        // getStateForPlacement.against is the against face. placing a torch will have it as UP. placing a bottom slab will have it as UP. placing a top slab will have it as DOWN.

        if (block instanceof BlockFence || (block instanceof BlockFenceGate && !state.getValue(BlockFenceGate.OPEN)) || block instanceof BlockWall) {
            builder.collisionHeight(1.5);
            fullyUnderstood = true;
        }

        if (block instanceof BlockTorch) { // includes redstone torch
            builder.canOnlyPlaceAgainst(Face.fromMC(state.getValue(BlockTorch.FACING)).opposite());
            fullyUnderstood = true;
        }

        if (block instanceof BlockShulkerBox) {
            builder.canOnlyPlaceAgainst(Face.fromMC(state.getValue(BlockShulkerBox.FACING)).opposite());
            builder.collisionHeight(1); // TODO should this be 1.5 because sometimes the shulker is open?
            fullyUnderstood = true;
        }

        if (block instanceof BlockFenceGate // because if we place it we need to think about hypothetically walking through it
                || block instanceof BlockLever
                || block instanceof BlockButton
                || block instanceof BlockBanner) {
            builder.placementLogicNotImplementedYet();
            fullyUnderstood = true;
        }

        if (block instanceof BlockSapling
                || block instanceof BlockRedstoneWire
                || block instanceof BlockRailBase
                || block instanceof BlockFlower
                || block instanceof BlockDeadBush
                || block instanceof BlockMushroom
        ) {
            builder.mustBePlacedBottomToTop();
            fullyUnderstood = true;
        }

        //isBlockNormalCube=true implies isFullCube=true
        if (state.isBlockNormalCube() || state.isFullBlock() || block instanceof BlockGlass || block instanceof BlockStainedGlass) {
            builder.canPlaceAgainstMe();
            fullyUnderstood = true;
        }

        if (state.isBlockNormalCube() || block instanceof BlockGlass || block instanceof BlockStainedGlass) {
            builder.collisionHeight(1);
            if (!(block instanceof BlockMagma || block instanceof BlockSlime)) {
                builder.fullyWalkableTop();
            }
            fullyUnderstood = true;
        }

        if (block instanceof BlockSnow) {
            fullyUnderstood = true;
            builder.fullyWalkableTop().collisionHeight(0.125 * (state.getValue(BlockSnow.LAYERS) - 1));
            // funny - if you have snow layers packed 8 high, it only supports the player to a height of 0.875, but it still counts as "isTopSolid" for placing stuff like torches on it
        }

        if (block instanceof BlockSoulSand) {
            builder.collisionHeight(0.875).fakeLessThanFullHeight();
            fullyUnderstood = true;
        }

        if (block instanceof BlockGrassPath || block instanceof BlockFarmland) {
            builder.collisionHeight(0.9375);
            fullyUnderstood = true;
        }


        // TODO fully walkable top and height
        if (fullyUnderstood) {
            return builder;
        } else {
            return null;
        }
    }
}
