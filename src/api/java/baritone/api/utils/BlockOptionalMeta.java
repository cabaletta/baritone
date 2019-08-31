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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

public final class BlockOptionalMeta {
    private final Block block;
    private final int meta;
    private final boolean noMeta;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");

    public BlockOptionalMeta(@Nonnull Block block, @Nullable Integer meta) {
        this.block = block;
        this.noMeta = isNull(meta);
        this.meta = noMeta ? 0 : meta;
    }

    public BlockOptionalMeta(@Nonnull Block block) {
        this(block, null);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        MatchResult matchResult = matcher.toMatchResult();
        noMeta = matchResult.group(2) == null;

        ResourceLocation id = new ResourceLocation(matchResult.group(1));

        if (!Block.REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Invalid block ID");
        }

        block = Block.REGISTRY.getObject(id);
        meta = noMeta ? 0 : Integer.parseInt(matchResult.group(2));
    }

    public Block getBlock() {
        return block;
    }

    public Integer getMeta() {
        return meta;
    }

    public boolean matches(@Nonnull Block block) {
        return block == this.block;
    }

    public boolean matches(@Nonnull IBlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && (noMeta || block.damageDropped(blockstate) == this.meta);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s,meta=%s}", block, meta);
    }

    public static IBlockState blockStateFromStack(ItemStack stack) {
        //noinspection deprecation
        return Block.getBlockFromItem(stack.getItem()).getStateFromMeta(stack.getMetadata());
    }
}
