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

package baritone.api.utils;

import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class BaritoneToast implements IToast
{
    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay;
    private long totalShowTime;

    public BaritoneToast(ITextComponent titleComponent, @Nullable ITextComponent subtitleComponent, long totalShowTime)
    {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.totalShowTime = totalShowTime;
    }

    public Visibility draw(GuiToast toastGui, long delta)
    {
        if (this.newDisplay)
        {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }

        toastGui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(10.0F, 100.0F, 50.0F);
        toastGui.drawTexturedModalRect(0, 0, 0, 96, 160, 32);

        if (this.subtitle == null)
        {
            toastGui.getMinecraft().fontRenderer.drawString(this.title, 18, 12, -11534256);
        }
        else
        {
            toastGui.getMinecraft().fontRenderer.drawString(this.title, 18, 7, -11534256);
            toastGui.getMinecraft().fontRenderer.drawString(this.subtitle, 18, 18, -16777216);
        }

        return delta - this.firstDrawTime < totalShowTime ? Visibility.SHOW : Visibility.HIDE;
    }

    public void setDisplayedText(ITextComponent titleComponent, @Nullable ITextComponent subtitleComponent)
    {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.newDisplay = true;
    }

    public static void addOrUpdate(GuiToast toast, ITextComponent title, @Nullable ITextComponent subtitle, long totalShowTime)
    {
        BaritoneToast baritonetoast = (BaritoneToast)toast.getToast(BaritoneToast.class, NO_TOKEN);

        if (baritonetoast == null)
        {
            toast.add(new BaritoneToast(title, subtitle, totalShowTime));
        }
        else
        {
            baritonetoast.setDisplayedText(title, subtitle);
        }
    }
}