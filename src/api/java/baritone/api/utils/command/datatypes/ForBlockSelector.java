package baritone.api.utils.command.datatypes;

import baritone.api.utils.BlockSelector;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class ForBlockSelector implements IDatatypeFor<BlockSelector> {
    public final BlockSelector selector;

    public ForBlockSelector() {
        selector = null;
    }

    public ForBlockSelector(ArgConsumer consumer) {
        selector = new BlockSelector(consumer.getS());
    }

    @Override
    public BlockSelector get() {
        return selector;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return consumer.tabCompleteDatatype(BlockById.class);
    }
}
