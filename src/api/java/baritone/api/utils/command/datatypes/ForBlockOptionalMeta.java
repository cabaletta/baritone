package baritone.api.utils.command.datatypes;

import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class ForBlockOptionalMeta implements IDatatypeFor<BlockOptionalMeta> {
    public final BlockOptionalMeta selector;

    public ForBlockOptionalMeta() {
        selector = null;
    }

    public ForBlockOptionalMeta(ArgConsumer consumer) {
        selector = new BlockOptionalMeta(consumer.getString());
    }

    @Override
    public BlockOptionalMeta get() {
        return selector;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return consumer.tabCompleteDatatype(BlockById.class);
    }
}
