package baritone.api.utils.command.datatypes;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

public class RelativeGoal implements IDatatypePost<Goal, BetterBlockPos> {
    final RelativeCoordinate[] coords;

    public RelativeGoal() {
        coords = new RelativeCoordinate[0];
    }

    public RelativeGoal(ArgConsumer consumer) {
        List<RelativeCoordinate> coordsList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            if (nonNull(consumer.peekDatatypeOrNull(RelativeCoordinate.class))) {
                coordsList.add(consumer.getDatatype(RelativeCoordinate.class));
            }
        }

        coords = coordsList.toArray(new RelativeCoordinate[0]);
    }

    @Override
    public Goal apply(BetterBlockPos origin) {
        switch (coords.length) {
            case 0:
                return new GoalBlock(origin);
            case 1:
                return new GoalYLevel(
                    coords[0].applyFloor(origin.y)
                );
            case 2:
                return new GoalXZ(
                    coords[0].applyFloor(origin.x),
                    coords[1].applyFloor(origin.z)
                );
            case 3:
                return new GoalBlock(
                    coords[0].applyFloor(origin.x),
                    coords[1].applyFloor(origin.y),
                    coords[2].applyFloor(origin.z)
                );
            default:
                throw new IllegalStateException("Unexpected coords size: " + coords.length);
        }
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return consumer.tabCompleteDatatype(RelativeCoordinate.class);
    }
}