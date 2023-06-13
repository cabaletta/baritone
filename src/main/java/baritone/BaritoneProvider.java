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

package baritone;

import baritone.api.IBaritone;
import baritone.api.IBaritoneProvider;
import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.IUserManager;
import baritone.api.cache.IWorldScanner;
import baritone.api.command.ICommandSystem;
import baritone.api.schematic.ISchematicSystem;
import baritone.bot.UserManager;
import baritone.cache.FasterWorldScanner;
import baritone.command.CommandSystem;
import baritone.utils.player.PrimaryPlayerContext;
import baritone.utils.schematic.SchematicSystem;

import java.util.AbstractList;
import java.util.List;

/**
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    private final IBaritone primary;
    private final List<IBaritone> all;

    public BaritoneProvider() {
        this.primary = new Baritone(PrimaryPlayerContext.INSTANCE);
        this.all = this.new BaritoneList();
    }

    @Override
    public IBaritone getPrimaryBaritone() {
        return this.primary;
    }

    @Override
    public List<IBaritone> getAllBaritones() {
        return this.all;
    }

    @Override
    public IWorldScanner getWorldScanner() {
        return FasterWorldScanner.INSTANCE;
    }

    @Override
    public IUserManager getUserManager() {
        return UserManager.INSTANCE;
    }

    @Override
    public ICommandSystem getCommandSystem() {
        return CommandSystem.INSTANCE;
    }

    @Override
    public ISchematicSystem getSchematicSystem() {
        return SchematicSystem.INSTANCE;
    }

    private final class BaritoneList extends AbstractList<IBaritone> {

        @Override
        public int size() {
            return 1 + this.getUsers().size();
        }

        @Override
        public IBaritone get(int index) {
            if (index < 0 || index >= this.size()) {
                throw new IndexOutOfBoundsException();
            }
            if (index == 0) {
                return BaritoneProvider.this.primary;
            }
            return this.getUsers().get(index - 1).getBaritone();
        }

        private List<IBaritoneUser> getUsers() {
            return BaritoneProvider.this.getUserManager().getUsers();
        }
    }
}
