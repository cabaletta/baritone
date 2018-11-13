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

package tenor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotTaskRegistry {
    Bot target;

    Map<Class<? extends ITask>, ITask<?>> singletonRegistry; // GetToCraftingTableTask

    Map<Class<? extends ITask>, Map<String, ITask<?>>> itemBased; // MineTask, AquireItemTask

    List<ITask<?>> allOthers; // AquireCraftingItems

    List<ITask<?>> totalList; // everything from all lists

    public BotTaskRegistry(Bot parent) {
        this.target = parent;
        this.singletonRegistry = new HashMap<>();
        this.itemBased = new HashMap<>();
        this.allOthers = new ArrayList<>();
        this.totalList = new ArrayList<>();
    }

    public void register(ITask<?> task) {
        if (task.bot() != target) {
            throw new IllegalStateException();
        }
        if (totalList.contains(task)) {
            throw new IllegalStateException();
        }
        totalList.add(task);
        allOthers.add(task);
    }

    public void registerSingleton(ITask<?> task) {
        if (!totalList.contains(task)) {
            throw new IllegalStateException();
        }
        if (!allOthers.contains(task)) {
            throw new IllegalStateException();
        }
        if (singletonRegistry.containsKey(task.getClass())) {
            throw new IllegalStateException();
        }
        allOthers.remove(task);
        singletonRegistry.put(task.getClass(), task);
    }

    public <T extends ITask<?>> T getSingleton(Class<T> klass) {
        if (singletonRegistry.containsKey(klass)) {
            return (T) singletonRegistry.get(klass);
        }
        try {
            T result = klass.getConstructor(Bot.class).newInstance(target);
            if (!singletonRegistry.containsKey(klass)) {
                throw new IllegalStateException();
            }
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    public void registerItemBased(ITask<?> task, String key) {
        if (!totalList.contains(task)) {
            throw new IllegalStateException();
        }
        if (!allOthers.contains(task)) {
            throw new IllegalStateException();
        }
        Map<String, ITask<?>> specificMap = itemBased.computeIfAbsent(task.getClass(), x -> new HashMap<>());
        if (specificMap.containsKey(key)) {
            throw new IllegalStateException();
        }
        allOthers.remove(task);
        specificMap.put(key, task);
    }

    public <T extends ITask<?>> T getItemBased(Class<T> klass, String key) {
        Map<String, ITask<?>> specificMap = itemBased.computeIfAbsent(klass, x -> new HashMap<>());
        if (specificMap.containsKey(key)) {
            return (T) specificMap.get(key);
        }
        try {
            T result = klass.getConstructor(Bot.class, String.class).newInstance(target, key);
            if (!specificMap.containsKey(key)) {
                throw new IllegalStateException();
            }
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
