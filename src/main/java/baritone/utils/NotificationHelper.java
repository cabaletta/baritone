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

public class NotificationHelper {
    public static void notify(String title, String text, boolean error) {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("");

            TrayIcon trayIcon = new TrayIcon(image, title);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip(title);
            tray.add(trayIcon);

            // Display Notification
            if(error)
                trayIcon.displayMessage(title, text, TrayIcon.MessageType.ERROR);
            else
                trayIcon.displayMessage(title, text, TrayIcon.MessageType.INFO);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
