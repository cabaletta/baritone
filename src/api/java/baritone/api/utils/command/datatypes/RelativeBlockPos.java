package baritone.api.utils.command.datatypes;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

import static java.util.Objects.isNull;

public class RelativeBlockPos implements IDatatypePost<BetterBlockPos, BetterBlockPos> {
    final RelativeCoordinate x;
    final RelativeCoordinate y;
    final RelativeCoordinate z;

    public RelativeBlockPos() {
        x = null;
        y = null;
        z = null;
    }

    public RelativeBlockPos(ArgConsumer consumer) {
        x = consumer.getDatatype(RelativeCoordinate.class);
        y = consumer.getDatatype(RelativeCoordinate.class);
        z = consumer.getDatatype(RelativeCoordinate.class);
    }

    @Override
    public BetterBlockPos apply(BetterBlockPos origin) {
        return new BetterBlockPos(
            x.apply((double) origin.x),
            y.apply((double) origin.y),
            z.apply((double) origin.z)
        );
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (consumer.has() && !consumer.has(4)) {
            while (consumer.has(2)) {
                if (isNull(consumer.peekDatatypeOrNull(RelativeCoordinate.class))) {
                    break;
                }

                consumer.get();
            }

            return consumer.tabCompleteDatatype(RelativeCoordinate.class);
        }

        return Stream.empty();
    }
}
