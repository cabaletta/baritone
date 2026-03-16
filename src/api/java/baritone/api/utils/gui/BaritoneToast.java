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

package baritone.api.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

public class BaritoneToast implements Toast {
    private static final Object TOKEN = new Object();

    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay;
    private long totalShowTime;
    private Visibility wantedVisibility = Visibility.SHOW;

    public BaritoneToast(Component titleComponent, Component subtitleComponent, long totalShowTime) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.totalShowTime = totalShowTime;
        this.newDisplay = true;
    }

    @Override
    public int width() {
        return 160;
    }

    @Override
    public int height() {
        return 32;
    }

    @Override
    public Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }
        this.wantedVisibility = delta - this.firstDrawTime < totalShowTime ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Font font, long delta) {
        guiGraphics.fill(0, 0, width(), height(), 0xFF2B2B2B);
        guiGraphics.fill(0, 0, width(), 1, 0xFFAA55FF);

        if (this.subtitle == null) {
            guiGraphics.drawString(font, this.title, 8, 12, 0xFFEEEEEE, false);
        } else {
            guiGraphics.drawString(font, this.title, 8, 7, 0xFFEEEEEE, false);
            guiGraphics.drawString(font, this.subtitle, 8, 18, 0xFFCCCCCC, false);
        }
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }

    public void setDisplayedText(Component titleComponent, Component subtitleComponent) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.newDisplay = true;
    }

    public static void addOrUpdate(ToastManager toast, Component title, Component subtitle, long totalShowTime) {
        BaritoneToast baritonetoast = toast.getToast(BaritoneToast.class, TOKEN);

        if (baritonetoast == null) {
            toast.addToast(new BaritoneToast(title, subtitle, totalShowTime));
        } else {
            baritonetoast.setDisplayedText(title, subtitle);
        }
    }

    public static void addOrUpdate(Component title, Component subtitle) {
        addOrUpdate(Minecraft.getInstance().getToastManager(), title, subtitle, baritone.api.BaritoneAPI.getSettings().toastTimer.value);
    }
}
