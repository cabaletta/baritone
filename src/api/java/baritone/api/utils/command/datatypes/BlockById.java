package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.stream.Stream;

public class BlockById implements IDatatypeFor<Block> {
    public final Block block;

    public BlockById() {
        block = null;
    }

    public BlockById(ArgConsumer consumer) {
        ResourceLocation id = new ResourceLocation(consumer.getS());

        if ((block = Block.REGISTRY.getObject(id)) == Blocks.AIR) {
            throw new RuntimeException("no block found by that id");
        }
    }

    @Override
    public Block get() {
        return block;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
            .append(
                Block.REGISTRY.getKeys()
                    .stream()
                    .map(Object::toString)
            )
            .filterPrefixNamespaced(consumer.getS())
            .sortAlphabetically()
            .stream();
    }
}
