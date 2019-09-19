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

package baritone.utils.command.defaults;

import baritone.api.Settings;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.EntityClassById;
import baritone.api.utils.command.datatypes.IDatatype;
import baritone.api.utils.command.datatypes.IDatatypeFor;
import baritone.api.utils.command.datatypes.PlayerByUsername;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class FollowCommand extends Command {
    public FollowCommand() {
        super("follow");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMin(1);

        FollowGroup group;
        FollowList list;
        List<Entity> entities = new ArrayList<>();
        List<Class<? extends Entity>> classes = new ArrayList<>();

        if (args.hasExactlyOne()) {
            baritone.getFollowProcess().follow((group = args.getEnum(FollowGroup.class)).filter);
        } else {
            args.requireMin(2);

            group = null;
            list = args.getEnum(FollowList.class);

            while (args.has()) {
                //noinspection unchecked
                Object gotten = args.getDatatypeFor(list.datatype);

                if (gotten instanceof Class) {
                    //noinspection unchecked
                    classes.add((Class<? extends Entity>) gotten);
                } else {
                    entities.add((Entity) gotten);
                }
            }

            baritone.getFollowProcess().follow(
                    classes.isEmpty()
                            ? entities::contains
                            : e -> classes.stream().anyMatch(c -> c.isInstance(e))
            );
        }

        if (nonNull(group)) {
            logDirect(String.format("Following all %s", group.name().toLowerCase(Locale.US)));
        } else {
            logDirect("Following these types of entities:");

            if (classes.isEmpty()) {
                entities.stream()
                        .map(Entity::toString)
                        .forEach(this::logDirect);
            } else {
                classes.stream()
                        .map(EntityList::getKey)
                        .map(Objects::requireNonNull)
                        .map(ResourceLocation::toString)
                        .forEach(this::logDirect);
            }
        }
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .append(FollowGroup.class)
                    .append(FollowList.class)
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            Class<? extends IDatatype> followType;

            try {
                followType = args.getEnum(FollowList.class).datatype;
            } catch (NullPointerException e) {
                return Stream.empty();
            }

            while (args.has(2)) {
                if (isNull(args.peekDatatypeOrNull(followType))) {
                    return Stream.empty();
                }

                args.get();
            }

            return args.tabCompleteDatatype(followType);
        }
    }

    @Override
    public String getShortDesc() {
        return "Follow entity things";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "The follow command tells Baritone to follow certain kinds of entities.",
                "",
                "Usage:",
                "> follow entities - Follows all entities.",
                "> follow entity <entity1> <entity2> <...> - Follow certain entities (for example 'skeleton', 'horse' etc.)",
                "> follow players - Follow players",
                "> follow player <username1> <username2> <...> - Follow certain players"
        );
    }

    private enum FollowGroup {
        ENTITIES(EntityLiving.class::isInstance),
        PLAYERS(EntityPlayer.class::isInstance); /* ,
        FRIENDLY(entity -> entity.getAttackTarget() != HELPER.mc.player),
        HOSTILE(FRIENDLY.filter.negate()); */

        final Predicate<Entity> filter;

        FollowGroup(Predicate<Entity> filter) {
            this.filter = filter;
        }
    }

    private enum FollowList {
        ENTITY(EntityClassById.class),
        PLAYER(PlayerByUsername.class);

        final Class<? extends IDatatypeFor> datatype;

        FollowList(Class<? extends IDatatypeFor> datatype) {
            this.datatype = datatype;
        }
    }
}
