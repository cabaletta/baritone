package baritone.api.utils.command.datatypes;

import baritone.api.BaritoneAPI;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

public class PlayerByUsername implements IDatatypeFor<EntityPlayer> {
    private final List<EntityPlayer> players =
        BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().playerEntities;
    public final EntityPlayer player;

    public PlayerByUsername() {
        player = null;
    }

    public PlayerByUsername(ArgConsumer consumer) {
        String username = consumer.getString();

        if (isNull(
            player = players
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null)
        )) {
            throw new RuntimeException("no player found by that username");
        }
    }

    @Override
    public EntityPlayer get() {
        return player;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
            .append(
                players
                    .stream()
                    .map(EntityPlayer::getName)
            )
            .filterPrefix(consumer.getString())
            .sortAlphabetically()
            .stream();
    }
}
