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

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.utils.SettingsUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small in-game configuration screen for the most frequently used pathing settings.
 * Settings are saved immediately whenever a control is changed.
 */
public final class BaritoneConfigScreen extends Screen {

    private static final int PANEL_WIDTH = 560;
    private static final int ROW_HEIGHT = 36;
    private static final int CONTENT_TOP = 112;
    private final Screen parent;
    private final List<ConfigOption> movementOptions = new ArrayList<>();
    private final List<ConfigOption> safetyOptions = new ArrayList<>();
    private final List<ConfigOption> searchOptions = new ArrayList<>();

    private int selectedTab;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;

    public BaritoneConfigScreen(Screen parent) {
        super(Component.literal("Baritone Settings"));
        this.parent = parent;
        createOptions();
    }

    private void createOptions() {
        Settings settings = BaritoneAPI.getSettings();

        movementOptions.add(new BooleanOption("Спринт", "Бежать, когда это возможно", settings.allowSprint));
        movementOptions.add(new BooleanOption("Паркур", "Перепрыгивать через разрывы", settings.allowParkour));
        movementOptions.add(new BooleanOption("Паркур вверх", "Разрешить прыжки при подъёме", settings.allowParkourAscend));
        movementOptions.add(new BooleanOption("Диагональный подъём", "Использовать диагональные подъёмы", settings.allowDiagonalAscend));
        movementOptions.add(new BooleanOption("Копать вниз", "Разрешить копать прямо под собой", settings.allowDownward));
        movementOptions.add(new BooleanOption("Ставить блоки", "Строить мосты и подниматься блоками", settings.allowPlace));

        safetyOptions.add(new BooleanOption("Ломать блоки", "Разрешить добывать блоки на пути", settings.allowBreak));
        safetyOptions.add(new BooleanOption("Водяное падение", "Использовать воду при дальнем падении", settings.allowWaterBucketFall));
        safetyOptions.add(new BooleanOption("Осторожно: песок/гравий", "Не провоцировать падение сыпучих блоков", settings.avoidUpdatingFallingBlocks));
        safetyOptions.add(new BooleanOption("Проверять жидкости", "Быть осторожнее при добыче рядом с жидкостями", settings.strictLiquidCheck));
        safetyOptions.add(new BooleanOption("Избегать мобов", "Выбирать путь подальше от мобов и спавнеров", settings.avoidance));
        safetyOptions.add(new IntegerOption("Падение без воды", "Максимальная высота падения", settings.maxFallHeightNoWater, 0, 16, 1));

        searchOptions.add(new DoubleOption("Эвристика пути", "Выше значение — быстрее поиск, но путь может быть хуже", settings.costHeuristic, 3.50, 4.00, 0.01));
        searchOptions.add(new DoubleOption("Цена установки", "Высокое значение помогает экономить блоки", settings.blockPlacementPenalty, 0, 40, 1));
        searchOptions.add(new DoubleOption("Цена добычи", "Насколько избегать ломания блоков", settings.blockBreakAdditionalPenalty, 0, 10, 0.5));
        searchOptions.add(new DoubleOption("Цена прыжка", "Насколько избегать лишних прыжков", settings.jumpPenalty, 0, 10, 0.5));
        searchOptions.add(new IntegerOption("Поиск у границы чанков", "Объём поиска за загруженными чанками", settings.pathingMaxChunkBorderFetch, 1, 200, 5));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, width - 24);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(12, (height - 360) / 2);
        rebuildControls();
    }

    private void rebuildControls() {
        clearWidgets();
        int tabY = panelTop + 49;
        int gap = 6;
        int tabWidth = (panelWidth - gap * 2) / 3;
        addTab("Движение", 0, panelLeft, tabY, tabWidth);
        addTab("Безопасность", 1, panelLeft + tabWidth + gap, tabY, tabWidth);
        addTab("Поиск пути", 2, panelLeft + (tabWidth + gap) * 2, tabY, tabWidth);

        List<ConfigOption> options = optionsForSelectedTab();
        int rowY = panelTop + CONTENT_TOP - 20;
        for (ConfigOption option : options) {
            addOptionControls(option, rowY);
            rowY += ROW_HEIGHT;
        }

        int footerY = panelTop + Math.max(300, CONTENT_TOP + options.size() * ROW_HEIGHT + 8);
        int buttonWidth = Math.min(150, (panelWidth - 24) / 3);
        addRenderableWidget(Button.builder(Component.literal("Сбросить"), button -> resetPage())
                .bounds(panelLeft + 8, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> saveSettings())
                .bounds(panelLeft + panelWidth / 2 - buttonWidth / 2, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), button -> onClose())
                .bounds(panelLeft + panelWidth - buttonWidth - 8, footerY, buttonWidth, 20).build());
    }

    private void addTab(String label, int tab, int x, int y, int tabWidth) {
        String shown = selectedTab == tab ? "• " + label : label;
        addRenderableWidget(Button.builder(Component.literal(shown), button -> {
            selectedTab = tab;
            rebuildControls();
        }).bounds(x, y, tabWidth, 20).build());
    }

    private void addOptionControls(ConfigOption option, int y) {
        int controlWidth = 36;
        int right = panelLeft + panelWidth - 10;
        if (option.isBoolean()) {
            addRenderableWidget(Button.builder(Component.literal(option.valueLabel()), button -> {
                option.toggle();
                saveSettings();
                rebuildControls();
            }).bounds(right - controlWidth, y, controlWidth, 20).build());
            return;
        }

        int valueWidth = 52;
        addRenderableWidget(Button.builder(Component.literal("−"), button -> change(option, -1))
                .bounds(right - controlWidth * 2 - valueWidth - 8, y, controlWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(option.valueLabel()), button -> {})
                .bounds(right - controlWidth - valueWidth - 4, y, valueWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> change(option, 1))
                .bounds(right - controlWidth, y, controlWidth, 20).build());
    }

    private void change(ConfigOption option, int direction) {
        option.adjust(direction);
        saveSettings();
        rebuildControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelHeight = Math.min(height - 24, 360);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE8101724);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, 0xFF55D6BE);
        graphics.drawCenteredString(font, "BARITONE  /  НАСТРОЙКИ БОТА", width / 2, panelTop + 15, 0xFFF3F6FA);
        graphics.drawCenteredString(font, "Изменения сохраняются автоматически", width / 2, panelTop + 31, 0xFF9EADBF);

        List<ConfigOption> options = optionsForSelectedTab();
        int rowY = panelTop + CONTENT_TOP - 15;
        for (ConfigOption option : options) {
            int controlsWidth = option.isBoolean() ? 36 : 132;
            int textWidth = Math.max(0, panelWidth - controlsWidth - 34);
            graphics.drawString(font, fit(option.label(), textWidth), panelLeft + 12, rowY, 0xFFEAF0F7, false);
            graphics.drawString(font, fit(option.description(), textWidth), panelLeft + 12, rowY + 13, 0xFF9EADBF, false);
            rowY += ROW_HEIGHT;
        }
        graphics.drawString(font, "Открыть снова: #config", panelLeft + 12, panelTop + 77, 0xFF8B9AAD, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? "" : result + "…";
    }

    private List<ConfigOption> optionsForSelectedTab() {
        return switch (selectedTab) {
            case 1 -> safetyOptions;
            case 2 -> searchOptions;
            default -> movementOptions;
        };
    }

    private void resetPage() {
        for (ConfigOption option : optionsForSelectedTab()) {
            option.reset();
        }
        saveSettings();
        rebuildControls();
    }

    private void saveSettings() {
        SettingsUtil.save(BaritoneAPI.getSettings());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private interface ConfigOption {
        String label();
        String description();
        String valueLabel();
        boolean isBoolean();
        void toggle();
        void adjust(int direction);
        void reset();
    }

    private static final class BooleanOption implements ConfigOption {
        private final String label;
        private final String description;
        private final Settings.Setting<Boolean> setting;

        private BooleanOption(String label, String description, Settings.Setting<Boolean> setting) {
            this.label = label;
            this.description = description;
            this.setting = setting;
        }

        @Override public String label() { return label; }
        @Override public String description() { return description; }
        @Override public String valueLabel() { return setting.value ? "ON" : "OFF"; }
        @Override public boolean isBoolean() { return true; }
        @Override public void toggle() { setting.value = !setting.value; }
        @Override public void adjust(int direction) { }
        @Override public void reset() { setting.reset(); }
    }

    private static final class DoubleOption implements ConfigOption {
        private final String label;
        private final String description;
        private final Settings.Setting<Double> setting;
        private final double minimum;
        private final double maximum;
        private final double step;

        private DoubleOption(String label, String description, Settings.Setting<Double> setting, double minimum, double maximum, double step) {
            this.label = label;
            this.description = description;
            this.setting = setting;
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
        }

        @Override public String label() { return label; }
        @Override public String description() { return description; }
        @Override public String valueLabel() { return String.format(Locale.ROOT, "%.2f", setting.value); }
        @Override public boolean isBoolean() { return false; }
        @Override public void toggle() { }
        @Override public void adjust(int direction) {
            setting.value = Math.max(minimum, Math.min(maximum, Math.round((setting.value + direction * step) * 100.0) / 100.0));
        }
        @Override public void reset() { setting.reset(); }
    }

    private static final class IntegerOption implements ConfigOption {
        private final String label;
        private final String description;
        private final Settings.Setting<Integer> setting;
        private final int minimum;
        private final int maximum;
        private final int step;

        private IntegerOption(String label, String description, Settings.Setting<Integer> setting, int minimum, int maximum, int step) {
            this.label = label;
            this.description = description;
            this.setting = setting;
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
        }

        @Override public String label() { return label; }
        @Override public String description() { return description; }
        @Override public String valueLabel() { return Integer.toString(setting.value); }
        @Override public boolean isBoolean() { return false; }
        @Override public void toggle() { }
        @Override public void adjust(int direction) { setting.value = Math.max(minimum, Math.min(maximum, setting.value + direction * step)); }
        @Override public void reset() { setting.reset(); }
    }
}
