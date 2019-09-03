package baritone.api.schematic;

import baritone.api.IBaritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.ISchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSchematic implements ISchematic {
    protected final IBaritone baritone;
    protected final IPlayerContext ctx;
    protected int x;
    protected int y;
    protected int z;

    public AbstractSchematic(@Nullable IBaritone baritone, int x, int y, int z) {
        this.baritone = baritone;
        this.ctx = baritone == null ? null : baritone.getPlayerContext();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int widthX() {
        return x;
    }

    @Override
    public int heightY() {
        return y;
    }

    @Override
    public int lengthZ() {
        return z;
    }

    protected IBlockState[] approxPlaceable() {
        EntityPlayerSP player = ctx.player();
        NonNullList<ItemStack> inventory = player.inventory.mainInventory;
        List<IBlockState> placeable = new ArrayList<>();
        placeable.add(Blocks.AIR.getDefaultState());

        // 27 + 9
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.get(i);

            if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock) {
                // <toxic cloud>
                placeable.add(((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(
                    ctx.world(),
                    ctx.playerFeet(),
                    EnumFacing.UP,
                    (float) player.posX,
                    (float) player.posY,
                    (float) player.posZ,
                    stack.getItem().getMetadata(stack.getMetadata()),
                    player
                ));
                // </toxic cloud>
            }
        }

        return placeable.toArray(new IBlockState[0]);
    }
}
