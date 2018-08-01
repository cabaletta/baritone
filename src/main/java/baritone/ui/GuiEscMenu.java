/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.ui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;

/**
 *
 * @author avecowa
 */
public class GuiEscMenu extends GuiScreen {
    private int field_146445_a;
    private int field_146444_f;
    /**
     * Adds the buttons (and other controls) to the screen in question. Called
     * when the GUI is displayed and when the window resizes, the buttonList is
     * cleared beforehand.
     */
    public void initGui() {
        this.field_146445_a = 0;
        this.buttonList.clear();
        int i = -16;
        int j = 98;
        int returnWidth = 130;
        int returnHeight = 20;
        this.buttonList.add(new GuiButton(1, /*this.width / 2 - returnWidth / 2*/ this.width - returnWidth - 10, 10 + 30, returnWidth, returnHeight, I18n.format("menu.returnToMenu", new Object[0])));
        this.buttonList.add(new GuiButton(0, /*this.width / 2 - returnWidth / 2*/ this.width - returnWidth - 10, 10, returnWidth, returnHeight, I18n.format("menu.options", new Object[0])));
        if (!this.mc.isIntegratedServerRunning()) {
            ((GuiButton) this.buttonList.get(0)).displayString = I18n.format("menu.disconnect", new Object[0]);
        }
        /* this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 24 + i, I18n.format("menu.returnToGame", new Object[0])));
         this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96 + i, 98, 20, I18n.format("menu.options", new Object[0])));
         GuiButton guibutton;
         this.buttonList.add(guibutton = new GuiButton(7, this.width / 2 + 2, this.height / 4 + 96 + i, 98, 20, I18n.format("menu.shareToLan", new Object[0])));
         this.buttonList.add(new GuiButton(5, this.width / 2 - 100, this.height / 4 + 48 + i, 98, 20, I18n.format("gui.achievements", new Object[0])));
         this.buttonList.add(new GuiButton(6, this.width / 2 + 2, this.height / 4 + 48 + i, 98, 20, I18n.format("gui.stats", new Object[0])));
         guibutton.enabled = this.mc.isSingleplayer() && !this.mc.getIntegratedServer().getPublic();*/
    }
    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed
     * for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new GuiIngameMenu());
                break;
            case 1:
                boolean flag = this.mc.isIntegratedServerRunning();
                boolean flag1 = this.mc.func_181540_al();
                button.enabled = false;
                this.mc.world.sendQuittingDisconnectingPacket();
                this.mc.loadWorld((WorldClient) null);
                if (flag) {
                    this.mc.displayGuiScreen(new GuiMainMenu());
                } else if (flag1) {
                    RealmsBridge realmsbridge = new RealmsBridge();
                    realmsbridge.switchToRealms(new GuiMainMenu());
                } else {
                    this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                }
            default:
                break;
        }
    }
    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen() {
        super.updateScreen();
        ++this.field_146444_f;
    }
    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY,
     * renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        //this.drawDefaultBackground(); NOPE
        this.drawCenteredString(this.fontRendererObj, "hit ESC to return to game", this.width / 2, 5, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
