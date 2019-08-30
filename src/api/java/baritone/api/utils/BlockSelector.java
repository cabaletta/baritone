package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

public class BlockSelector implements IBlockFilter {
    private final Block block;
    private final IBlockState blockstate;
    private final int damage;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");

    public BlockSelector(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new RuntimeException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();
        boolean hasData = matchResult.groupCount() > 1;

        ResourceLocation id = new ResourceLocation(matchResult.group(1));

        if (!Block.REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Invalid block ID");
        }

        block = Block.REGISTRY.getObject(id);
        //noinspection deprecation
        blockstate = hasData ? block.getStateFromMeta(Integer.parseInt(matchResult.group(2))) : null;
        damage = block.damageDropped(blockstate);
    }

    @Override
    public boolean selected(@Nonnull IBlockState blockstate) {
        return blockstate.getBlock() == block && (isNull(this.blockstate) || block.damageDropped(blockstate) == damage);
    }

    @Override
    public List<Block> blocks() {
        return Collections.singletonList(block);
    }

    @Override
    public String toString() {
        return String.format("BlockSelector{block=%s,blockstate=%s}", block, blockstate);
    }

    public static IBlockState stateFromItem(ItemStack stack) {
        //noinspection deprecation
        return Block.getBlockFromItem(stack.getItem()).getStateFromMeta(stack.getMetadata());
    }
}
