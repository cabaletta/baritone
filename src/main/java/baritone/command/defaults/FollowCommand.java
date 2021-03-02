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

package baritone.command.defaults;

import baritone.KeepName;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.EntityClassById;
import baritone.api.command.datatypes.IDatatypeFor;
import baritone.api.command.datatypes.ItemById;
import baritone.api.command.datatypes.NearbyPlayer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FollowCommand extends Command {

    public FollowCommand(IBaritone baritone) {
        super(baritone, "follow");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        FollowGroup group;
        FollowList list;
        Predicate<Entity> filter = (e -> false);
        List<Object> following = new ArrayList<>();
        if (args.hasExactlyOne()) {
            list = null;
            baritone.getFollowProcess().follow((group = args.getEnum(FollowGroup.class)).filter);
        } else {
            args.requireMin(2);
            group = null;
            list = args.getEnum(FollowList.class);
            while (args.hasAny()) {
                Object gotten = args.getDatatypeFor(list.datatype);
                filter = filter.or(list.filterFor.apply(gotten));
                following.add(gotten);
            }
            baritone.getFollowProcess().follow(filter);
        }
        if (group != null) {
            logDirect(String.format("Following all %s", group.name().toLowerCase(Locale.US)));
        } else {
            logDirect("Following these types of entities:");
            following.stream().map(list.toString::apply).forEach(this::logDirect);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .append(FollowGroup.class)
                    .append(FollowList.class)
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            IDatatypeFor followType;
            try {
                followType = args.getEnum(FollowList.class).datatype;
            } catch (NullPointerException e) {
                return Stream.empty();
            }
            while (args.has(2)) {
                if (args.peekDatatypeOrNull(followType) == null) {
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
        return Arrays.asList(
                "The follow command tells Baritone to follow certain kinds of entities.",
                "",
                "Usage:",
                "> follow entities - Follows all entities.",
                "> follow entity <entity1> <entity2> <...> - Follow certain entities (for example 'skeleton', 'horse' etc.)",
                "> follow items - Follow all item entities",
                "> follow item <item1> <item2> <...> - Follow certain items (for example 'carrot', 'bone' etc.)",
                "> follow players - Follow players",
                "> follow player <username1> <username2> <...> - Follow certain players"
        );
    }

    @KeepName
    private enum FollowGroup {
        ENTITIES(EntityLiving.class::isInstance),
        ITEMS(EntityItem.class::isInstance),
        PLAYERS(EntityPlayer.class::isInstance); /* ,
        FRIENDLY(entity -> entity.getAttackTarget() != HELPER.mc.player),
        HOSTILE(FRIENDLY.filter.negate()); */
        final Predicate<Entity> filter;

        FollowGroup(Predicate<Entity> filter) {
            this.filter = filter;
        }
    }

    @KeepName
    private enum FollowList {
        ENTITY(EntityClassById.INSTANCE,
                c -> Objects.requireNonNull(EntityList.getKey((Class<? extends Entity>) c)),
                c -> ((Class<? extends Entity>) c)::isInstance
        ),
        ITEM(ItemById.INSTANCE,
                i -> Item.REGISTRY.getNameForObject((Item) i),
                i -> (e -> e instanceof EntityItem && ((EntityItem) e).getItem().getItem().equals(i))
        ),
        PLAYER(NearbyPlayer.INSTANCE, e -> e, e -> e::equals);

        final IDatatypeFor datatype;
        final Function<Object, String> toString;
        final Function<Object, Predicate<Entity>> filterFor;

        FollowList(IDatatypeFor datatype, Function<Object, Object> toString, Function<Object, Predicate<Entity>> filterFor) {
            this.datatype = datatype;
            this.toString = toString.andThen(Object::toString);
            this.filterFor = filterFor;
        }

    }
}
