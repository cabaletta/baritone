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

package baritone.api.command.registry;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This registry class allows for registration and unregistration of a certain type. This is mainly designed for use by
 * event handlers where newly registered ones are encountered first during iteration and can therefore override older
 * ones. In Baritone, this is used for commands and argument parsers so that mods and addons can extend Baritone's
 * functionality without resorting to hacks, wrappers, or mixins.
 *
 * @param <V> The entry type that will be stored in this registry. This can be anything, really - preferably anything
 *            that works as a HashMap key, as that's what's used to keep track of which entries are registered or not.
 */
public class Registry<V> {

    /**
     * An internal linked list of all the entries that are currently registered. This is a linked list so that entries
     * can be inserted at the beginning, which means that newer entries are encountered first during iteration. This is
     * an important property of the registry that makes it more useful than a simple list, and also the reason it does
     * not just use a map.
     */
    private final Deque<V> _entries = new LinkedList<>();
    /**
     * A HashSet containing every entry currently registered. Entries are added to this set when something is registered
     * and removed from the set when they are unregistered. An entry being present in this set indicates that it is
     * currently registered, can be removed, and should not be reregistered until it is removed.
     */
    private final Set<V> registered = new HashSet<>();
    /**
     * The collection of entries that are currently in this registry. This is a collection (and not a list) because,
     * internally, entries are stored in a linked list, which is not the same as a normal list.
     */
    public final Collection<V> entries = Collections.unmodifiableCollection(_entries);

    /**
     * @param entry The entry to check.
     * @return If this entry is currently registered in this registry.
     */
    public boolean registered(V entry) {
        return registered.contains(entry);
    }

    /**
     * Ensures that the entry {@code entry} is registered.
     *
     * @param entry The entry to register.
     * @return A boolean indicating whether or not this is a new registration. No matter the value of this boolean, the
     * entry is always guaranteed to now be in this registry. This boolean simply indicates if the entry was <i>not</i>
     * in the map prior to this method call.
     */
    public boolean register(V entry) {
        if (!registered(entry)) {
            _entries.addFirst(entry);
            registered.add(entry);
            return true;
        }
        return false;
    }

    /**
     * Unregisters this entry from this registry. After this method call, the entry is guaranteed to be removed from the
     * registry, since each entry only ever appears once.
     *
     * @param entry The entry to unregister.
     */
    public void unregister(V entry) {
        if (!registered(entry)) {
            return;
        }
        _entries.remove(entry);
        registered.remove(entry);
    }

    /**
     * Returns an iterator that iterates over each entry in this registry, with the newest elements iterated over first.
     * Internally, as new elements are prepended to the registry rather than appended to the end, this order is the best
     * way to search through the registry if you want to discover newer items first.
     */
    public Iterator<V> iterator() {
        return _entries.iterator();
    }

    /**
     * Returns an iterator that iterates over each entry in this registry, in the order they were added. Internally,
     * this iterates through the registry backwards, as new elements are prepended to the registry rather than appended
     * to the end. You should only do this when you need to, for example, list elements in order - it is almost always
     * fine to simply use {@link Iterable#forEach(Consumer) forEach} on the {@link #entries} collection instead.
     */
    public Iterator<V> descendingIterator() {
        return _entries.descendingIterator();
    }

    /**
     * Returns a stream that contains each entry in this registry, with the newest elements ordered first. Internally,
     * as new elements are prepended to the registry rather than appended to the end, this order is the best way to
     * search through the registry if you want to discover newer items first.
     */
    public Stream<V> stream() {
        return _entries.stream();
    }

    /**
     * Returns a stream that returns each entry in this registry, in the order they were added. Internally, this orders
     * the registry backwards, as new elements are prepended to the registry rather than appended to the end. You should
     * only use this when you need to, for example, list elements in order - it is almost always fine to simply use the
     * regular {@link #stream()} method instead.
     */
    public Stream<V> descendingStream() {
        return StreamSupport.stream(Spliterators.spliterator(
                descendingIterator(),
                _entries.size(),
                Spliterator.SIZED | Spliterator.SUBSIZED
        ), false);
    }
}
