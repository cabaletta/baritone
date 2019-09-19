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

package baritone.api.utils.command.argparser;

public abstract class ArgParser<T> implements IArgParser<T> {
    private final Class<T> klass;

    protected ArgParser(Class<T> klass) {
        this.klass = klass;
    }

    @Override
    public Class<T> getKlass() {
        return klass;
    }

    public static abstract class Stated<T, S> extends ArgParser<T> implements IArgParser.Stated<T, S> {
        private final Class<S> stateKlass;

        protected Stated(Class<T> klass, Class<S> stateKlass) {
            super(klass);
            this.stateKlass = stateKlass;
        }

        @Override
        public Class<S> getStateKlass() {
            return stateKlass;
        }
    }
}
