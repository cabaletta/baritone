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

package baritone.install;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Stack;

public class GsonJanker {
    private static final URLClassLoader classLoader;

    static {
        try {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar");
            File file = new File("gson-2.8.5.jar");
            if (!file.exists()) {
                Files.copy(url.openStream(), file.toPath());
                //tells java to delete the file when the program exits
                file.deleteOnExit();
            }
            classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object head = null;
    public Stack<Object> current = new Stack<>();

    public GsonJanker(String content) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // read to json element with Jsonparser
        Class<?> clazz = classLoader.loadClass("com.google.gson.JsonParser");
        head = clazz.getMethod("parse", String.class).invoke(clazz.getConstructor().newInstance(), content);
        current.push(head);
    }


    public void selectOrAddObject(String element) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        // get current as json object and test if it has "element"
        Class<?> clazz = current.peek().getClass();
        if ((Boolean) clazz.getMethod("has", String.class).invoke(current.peek(), element)) {
            // if it has, get it
            current.push(clazz.getMethod("get", String.class).invoke(current.peek(), element));
        } else {
            // if it doesn't, add it
            current.push(clazz.getMethod("add", String.class, classLoader.loadClass("com.google.gson.JsonElement")).invoke(current.peek(), element, classLoader.loadClass("com.google.gson.JsonObject").getConstructor().newInstance()));
        }
    }

    public void addObject(String element, GsonJanker content) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // get current as json object and set "element" to "content"
        Class<?> clazz = current.peek().getClass();
        clazz.getMethod("add", String.class, classLoader.loadClass("com.google.gson.JsonElement")).invoke(current.peek(), element, content.head);
    }

    public void appendToArray(GsonJanker content) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // get current as json array and add content
        Class<?> clazz = current.peek().getClass();
        clazz.getMethod("add", classLoader.loadClass("com.google.gson.JsonElement")).invoke(current.peek(), content.head);
    }

    public void add(String element, String value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // get current as json object and set "element" to "value"
        Class<?> clazz = current.peek().getClass();
        clazz.getMethod("addProperty", String.class, String.class).invoke(current.peek(), element, value);
    }

    public void pop() {
        // pop current
        current.pop();
    }

    public String serialize() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // serialize head
        Class<?> clazz = head.getClass();
        return (String) clazz.getMethod("toString").invoke(head);
    }
}
