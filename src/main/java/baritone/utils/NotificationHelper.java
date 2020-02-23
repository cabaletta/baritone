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

package baritone.utils;

import java.awt.*;
import java.io.IOException;

/**
 * This class is not called from the main game thread.
 * Do not refer to any Minecraft classes, it wouldn't be thread safe.
 *
 * @author aUniqueUser
 */
public class NotificationHelper {

    public static void notify(String text, boolean error) {
        if (System.getProperty("os.name").contains("Linux")) {
            linux(text);
        } else {
            notification(text, error);
        }
    }

    public static void notification(String text, boolean error) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage("");

                TrayIcon trayIcon = new TrayIcon(image, "Baritone");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("Baritone");
                tray.add(trayIcon);

                if (error) {
                    trayIcon.displayMessage("Baritone", text, TrayIcon.MessageType.ERROR);
                } else {
                    trayIcon.displayMessage("Baritone", text, TrayIcon.MessageType.INFO);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("SystemTray is not supported");
        }
    }

    /*
    * The only way to display notifications on linux is to use the java-gnome library,
    * or send notify-send to shell with a ProcessBuilder. Unfortunately the java-gnome
    * library is licenced under the GPL, see (https://en.wikipedia.org/wiki/Java-gnome)
    */
    public static void linux(String text) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("notify-send", "-a", "Baritone", text);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
