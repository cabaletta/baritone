package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.util.EnumFacing;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class ForEnumFacing implements IDatatypeFor<EnumFacing> {
    private final EnumFacing facing;

    public ForEnumFacing() {
        facing = null;
    }

    public ForEnumFacing(ArgConsumer consumer) {
        facing = EnumFacing.valueOf(consumer.getString().toUpperCase(Locale.US));
    }

    @Override
    public EnumFacing get() {
        return facing;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
            .append(
                Arrays.stream(EnumFacing.values())
                    .map(EnumFacing::getName)
                    .map(String::toLowerCase)
            )
            .filterPrefix(consumer.getString())
            .stream();
    }
}
