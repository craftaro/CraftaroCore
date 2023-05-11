package com.songoda.core.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.configuration.ConfigEntry;
import com.songoda.core.configuration.songoda.SongodaYamlConfig;
import com.songoda.core.gui.methods.Clickable;
import com.songoda.core.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomizableGui extends Gui {
    private static boolean showGuiKeys = false;
    private int activationCount = 0;

    private static final Map<String, CustomContent> loadedGuis = new HashMap<>();
    private final CustomContent customContent;

    public CustomizableGui(Plugin plugin, String guiKey) {
        this(plugin, guiKey, null);
    }

    public CustomizableGui(@NotNull Plugin plugin, @NotNull String guiKey, @Nullable Gui parent) {
        super(parent);

        if (!loadedGuis.containsKey(guiKey) || showGuiKeys) {
            File localeFolder = new File(plugin.getDataFolder(), "gui/");

            if (!localeFolder.exists()) {
                localeFolder.mkdir();
            }

            SongodaYamlConfig config = new SongodaYamlConfig(new File(plugin.getDataFolder(), "gui/" + guiKey + ".yml"));
            try {
                config.load();
            } catch (IOException ex) {
                // FIXME
                throw new RuntimeException(ex);
            }

            config.setNodeComment("overrides", "For information on how to apply overrides please visit\n" +
                    "https://wiki.craftaro.com/index.php/Gui");
            config.setNodeComment("overrides.example", "This is just an example and does not override to any items in this GUI.");

            config.createEntry("overrides.example.item", CompatibleMaterial.STONE)
                    .withComment("This is the icon material you would like to replace\n" +
                            "the current material with.");
            config.createEntry("overrides.example.position", 5)
                    .withComment("This is the current position of the icon you would like to move.\n" +
                            "The number represents the cell the icon currently resides in.");

            ConfigEntry disabledGuis = config.createEntry("disabled", Arrays.asList("example3", "example4", "example5"))
                    .withComment("All keys on this list will be disabled. You can add any items key here\n" +
                            "if you no longer want that item in the GUI.");

            try {
                config.save();
            } catch (IOException ex) {
                // FIXME
                throw new RuntimeException(ex);
            }

            CustomContent customContent = loadedGuis.computeIfAbsent(guiKey, g -> new CustomContent(guiKey));
            loadedGuis.put(guiKey, customContent);
            this.customContent = customContent;

            int rows = config.getReadEntry("overrides.__ROWS__").getIntOr(-1);
            if (rows != -1) {
                customContent.setRows(rows);
            }

            for (String overrideKey : config.getKeys("overrides")) {
                String keyPrefix = "overrides." + overrideKey;

                ConfigEntry title = config.getReadEntry(keyPrefix + ".title");

                ConfigEntry position = config.getReadEntry(keyPrefix + ".position");

                ConfigEntry row = config.getReadEntry(keyPrefix + ".row");
                ConfigEntry col = config.getReadEntry(keyPrefix + ".col");

                ConfigEntry mirrorRow = config.getReadEntry(keyPrefix + ".mirrorrow");
                ConfigEntry mirrorCol = config.getReadEntry(keyPrefix + ".mirrorcol");

                ConfigEntry item = config.getReadEntry(keyPrefix + ".item");
                ConfigEntry lore = config.getReadEntry(keyPrefix + ".lore");

                boolean configHasRowOrColSet = row.has() || col.has();
                boolean configHasMirrorRowOrColSet = mirrorRow.has() || mirrorCol.has();

                if (configHasMirrorRowOrColSet) {
                    customContent.addButton(overrideKey,
                            row.getIntOr(-1),
                            col.getIntOr(-1),
                            mirrorRow.getBoolean(),
                            mirrorCol.getBoolean(),
                            item.getMaterial());
                } else if (configHasRowOrColSet) {
                    customContent.addButton(overrideKey,
                            row.getIntOr(-1),
                            col.getIntOr(-1),
                            title.getString(),
                            lore.getStringList(),
                            item.getMaterial());
                } else {
                    customContent.addButton(overrideKey,
                            position.getStringOr("-1"),
                            title.getString(),
                            lore.getStringList(),
                            item.getMaterial());

                }
            }

            for (String disabled : disabledGuis.getStringListOr(Collections.emptyList())) {
                customContent.disableButton(disabled);
            }
        } else {
            customContent = loadedGuis.get(guiKey);
        }

        setPrivateDefaultAction(event -> {
            if (event.clickType == ClickType.SHIFT_RIGHT) {
                activationCount++;
            }

            if (activationCount >= 8 && event.player.hasPermission("songoda.admin")) {
                showGuiKeys = !showGuiKeys;
                activationCount = 0;

                event.player.sendMessage("Gui keys " + (showGuiKeys ? "enabled" : "disabled") + ".");
            }
        });

        if (customContent.isButtonCustomized("__DEFAULT__")) {
            blankItem = GuiUtils.getBorderItem(customContent.getCustomizedButton("__DEFAULT__").item);
        }
    }

    @NotNull
    public Gui setRows(int rows) {
        int customRows = customContent.getRows();

        return super.setRows(customRows != -1 ? customRows : rows);
    }

    @NotNull
    protected Inventory generateInventory(@NotNull GuiManager manager) {
        applyCustomItems();

        return super.generateInventory(manager);
    }

    public void update() {
        applyCustomItems();
        super.update();
    }

    private void applyCustomItems() {
        for (CustomButton customButton : customContent.getCustomButtons().values()) {
            if (customButton instanceof MirrorFill) {
                applyCustomItem(customButton);
            }
        }

        for (CustomButton customButton : customContent.getCustomButtons().values()) {
            if (!(customButton instanceof MirrorFill)) {
                applyCustomItem(customButton);
            }
        }
    }

    private void applyCustomItem(CustomButton customButton) {
        if (customButton.row != -1 && customButton.col != -1) {
            if (customButton instanceof MirrorFill) {
                mirrorFill(customButton.key, customButton.row, customButton.col,
                        ((MirrorFill) customButton).mirrorRow, ((MirrorFill) customButton).mirrorCol,
                        customButton.createItem());
            } else {
                setItem(customButton.key, customButton.row, customButton.col, customButton.createItem());
            }
        } else {
            for (Integer position : customButton.positions) {
                setItem(customButton.key, position, customButton.createItem());
            }
        }
    }

    @NotNull
    public Gui setDefaultItem(@Nullable ItemStack item) {
        if (item == null) {
            return this;
        }

        applyShowGuiKeys("__DEFAULT__", item);

        if (customContent.isButtonCustomized("__DEFAULT__")) {
            return this;
        }

        return super.setDefaultItem(item);
    }

    @NotNull
    public Gui setItem(@NotNull String key, int cell, @Nullable ItemStack item) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        applyShowGuiKeys(key, item);

        if (customContent.isButtonCustomized(key)) {
            CustomButton btn = customContent.getCustomizedButton(key);
            cells = btn.applyPosition(cell);
            btn.applyItem(item);
        }

        for (int c : cells) {
            setItem(c, item);
        }

        return this;
    }

    @NotNull
    public Gui setItem(@NotNull String key, int row, int col, @Nullable ItemStack item) {
        final int cell = col + row * inventoryType.columns;

        return setItem(key, cell, item);
    }

    public Gui mirrorFill(@NotNull String key, int row, int col, boolean mirrorRow, boolean mirrorCol, @NotNull ItemStack item) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        ItemStack newItem = item.clone();
        boolean isShow = applyShowGuiKeys(key, newItem);

        if (customContent.isButtonCustomized(key)) {
            CustomButton btn = customContent.getCustomizedButton(key);
            row = btn.applyPositionRow(row);
            col = btn.applyPositionCol(col);

            if (btn.applyItem(newItem)) {
                isShow = true;
            }

            if (btn instanceof MirrorFill) {
                MirrorFill mf = (MirrorFill) btn;
                mirrorRow = mf.mirrorRow;
                mirrorCol = mf.mirrorCol;
            }
        }

        return mirrorFill(row, col, mirrorRow, mirrorCol, isShow ? newItem : item);
    }

    @NotNull
    public Gui highlightItem(@NotNull String key, int cell) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            highlightItem(c);
        }

        return this;
    }

    @NotNull
    public Gui highlightItem(@NotNull String key, int row, int col) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        final int cell = col + row * inventoryType.columns;

        return highlightItem(key, cell);
    }

    @NotNull
    public Gui removeHighlight(@NotNull String key, int cell) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            removeHighlight(c);
        }

        return this;
    }

    @NotNull
    public Gui removeHighlight(@NotNull String key, int row, int col) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        final int cell = col + row * inventoryType.columns;

        return removeHighlight(key, cell);
    }

    @NotNull
    public Gui updateItemLore(@NotNull String key, int row, int col, @NotNull String... lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItemLore(key, col + row * inventoryType.columns, lore);
    }

    @NotNull
    public Gui updateItemLore(@NotNull String key, int cell, @NotNull String... lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItemLore(c, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItemLore(@NotNull String key, int row, int col, @Nullable List<String> lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItemLore(key, col + row * inventoryType.columns, lore);
    }

    @NotNull
    public Gui updateItemLore(@NotNull String key, int cell, @Nullable List<String> lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItemLore(c, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItemName(@NotNull String key, int row, int col, @Nullable String name) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItemName(key, col + row * inventoryType.columns, name);
    }

    @NotNull
    public Gui updateItemName(@NotNull String key, int cell, @Nullable String name) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItemName(c, name);
        }

        return this;
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @Nullable String name, @NotNull String... lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, name, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @Nullable String name, @NotNull String... lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, cell, name, Arrays.asList(lore));
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @Nullable String name, @Nullable List<String> lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, name, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @NotNull String name, @Nullable List<String> lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        lore = applyShowGuiKeys(key, lore);

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItem(c, name, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @NotNull ItemStack itemTo, @Nullable String title, @NotNull String... lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @NotNull ItemStack itemTo, @Nullable String title, @NotNull String... lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItem(c, itemTo, title, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @NotNull CompatibleMaterial itemTo, @Nullable String title, @NotNull String... lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @NotNull CompatibleMaterial itemTo, @Nullable String title, @Nullable String... lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItem(key, c, itemTo, title, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @NotNull ItemStack itemTo, @Nullable String title, @Nullable List<String> lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @NotNull ItemStack itemTo, @Nullable String title, @Nullable List<String> lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItem(key, c, itemTo, title, lore);
        }

        return this;
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int row, int col, @NotNull CompatibleMaterial itemTo, @Nullable String title, @Nullable List<String> lore) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return updateItem(key, col + row * inventoryType.columns, itemTo, title, lore);
    }

    @NotNull
    public Gui updateItem(@NotNull String key, int cell, @NotNull CompatibleMaterial itemTo, @Nullable String title, @Nullable List<String> lore) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            updateItem(key, c, itemTo, title, lore);
        }

        return this;
    }

    @NotNull
    public Gui setAction(@NotNull String key, int cell, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        setConditional(key, cell, null, action);

        return this;
    }

    @NotNull
    public Gui setAction(@NotNull String key, int row, int col, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        setConditional(key, col + row * inventoryType.columns, null, action);

        return this;
    }

    @NotNull
    public Gui setAction(@NotNull String key, int cell, @Nullable ClickType type, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        setConditional(key, cell, type, action);

        return this;
    }

    @NotNull
    public Gui setAction(@NotNull String key, int row, int col, @Nullable ClickType type, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        setConditional(key, col + row * inventoryType.columns, type, action);

        return this;
    }

    @NotNull
    public Gui clearActions(@NotNull String key, int cell) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            clearActions(c);
        }

        return this;
    }

    @NotNull
    public Gui clearActions(@NotNull String key, int row, int col) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return clearActions(key, col + row * inventoryType.columns);
    }

    @NotNull
    public Gui setButton(@NotNull String key, int cell, ItemStack item, @Nullable Clickable action) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        applyShowGuiKeys(key, item);

        if (customContent.isButtonCustomized(key)) {
            CustomButton btn = customContent.getCustomizedButton(key);
            cells = btn.applyPosition(cell);
            btn.applyItem(item);
        }

        for (int c : cells) {
            setItem(c, item);
            setConditional(c, null, action);
        }

        return this;
    }

    @NotNull
    public Gui setButton(@NotNull String key, int row, int col, @Nullable ItemStack item, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return setButton(key, col + row * inventoryType.columns, item, action);
    }

    @NotNull
    public Gui setButton(@NotNull String key, int cell, @Nullable ItemStack item, @Nullable ClickType type, @Nullable Clickable action) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        applyShowGuiKeys(key, item);

        if (customContent.isButtonCustomized(key)) {
            CustomButton btn = customContent.getCustomizedButton(key);
            cells = btn.applyPosition(cell);
            btn.applyItem(item);
        }

        for (int c : cells) {
            setButton(c, item, type, action);
        }

        return this;
    }

    @NotNull
    public Gui setButton(@NotNull String key, int row, int col, @Nullable ItemStack item, @Nullable ClickType type, @Nullable Clickable action) {
        if (customContent.isButtonDisabled(key)) {
            return this;
        }

        return setButton(key, col + row + inventoryType.columns, item, type, action);
    }

    protected void setConditional(@NotNull String key, int cell, @Nullable ClickType type, @Nullable Clickable action) {
        List<Integer> cells = Collections.singletonList(cell);

        if (customContent.isButtonDisabled(key)) {
            return;
        }

        if (customContent.isButtonCustomized(key)) {
            cells = customContent.getCustomizedButton(key).applyPosition(cell);
        }

        for (int c : cells) {
            setConditional(c, type, action);
        }
    }

    public Gui setNextPage(ItemStack item) {
        applyShowGuiKeys("__NEXT__", item);

        if (customContent.isButtonCustomized("__NEXT__")) {
            customContent.getCustomizedButton("__NEXT__").applyItem(item);
        }

        return super.setNextPage(item);
    }

    public Gui setPrevPage(ItemStack item) {
        applyShowGuiKeys("__PREV__", item);

        if (customContent.isButtonCustomized("__PREV__")) {
            customContent.getCustomizedButton("__PREV__").applyItem(item);
        }

        return super.setPrevPage(item);
    }

    @NotNull
    public Gui setNextPage(int cell, @NotNull ItemStack item) {
        List<Integer> cells = Collections.singletonList(cell);

        applyShowGuiKeys("__NEXT__", item);

        if (customContent.isButtonCustomized("__NEXT__")) {
            CustomButton btn = customContent.getCustomizedButton("__NEXT__");
            cells = btn.applyPosition(cell);
            btn.applyItem(item);
        }

        for (int c : cells) {
            return super.setNextPage(c, item);
        }

        return this;
    }

    @NotNull
    public Gui setNextPage(int row, int col, @NotNull ItemStack item) {
        applyShowGuiKeys("__NEXT__", item);

        return setNextPage(col + row * inventoryType.columns, item);
    }

    @NotNull
    public Gui setPrevPage(int cell, @NotNull ItemStack item) {
        List<Integer> cells = Collections.singletonList(cell);

        applyShowGuiKeys("__PREV__", item);

        if (customContent.isButtonCustomized("__PREV__")) {
            CustomButton btn = customContent.getCustomizedButton("__PREV__");
            cells = btn.applyPosition(cell);
            btn.applyItem(item);
        }

        for (int c : cells) {
            super.setPrevPage(c, item);
        }

        return this;
    }

    @NotNull
    public Gui setPrevPage(int row, int col, @NotNull ItemStack item) {
        applyShowGuiKeys("__PREV__", item);

        return setPrevPage(col + row * inventoryType.columns, item);
    }

    private boolean applyShowGuiKeys(String key, ItemStack item) {
        if (!showGuiKeys) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }

        List<String> lore = new ArrayList<>(Collections.singletonList("Key: " + key));

        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return true;
    }

    private List<String> applyShowGuiKeys(String key, List<String> lore) {
        if (!showGuiKeys) {
            return lore;
        }

        List<String> newLore = new ArrayList<>(Collections.singletonList("Key: " + key));
        newLore.addAll(lore);

        return newLore;
    }

    private class CustomButton {
        private final String key;

        private final List<Integer> positions;
        private final int row;
        private final int col;

        private final String title;
        private final List<String> lore;

        private final CompatibleMaterial item;

        public CustomButton(String key, List<Integer> positions, String title, List<String> lore, CompatibleMaterial item) {
            this.key = key;
            this.positions = positions;
            this.row = -1;
            this.col = -1;
            this.item = item;
            this.title = title;
            this.lore = lore;
        }

        public CustomButton(String key, int row, int col, String title, List<String> lore, CompatibleMaterial item) {
            this.key = key;
            this.positions = null;
            this.row = row;
            this.col = col;
            this.item = item;
            this.title = title;
            this.lore = lore;
        }

        public String getKey() {
            return key;
        }

        public boolean applyItem(ItemStack item) {
            if (item == null) {
                return false;
            }

            item.setType(this.item.getMaterial());

            if (ServerVersion.isServerVersionAtOrBelow(ServerVersion.V1_13)) {
                item.setDurability(this.item.getData());
            }

            applyMeta(item);

            return true;
        }

        public ItemStack createItem() {
            ItemStack item = this.item.getItem();
            applyMeta(item);

            return item;
        }

        private void applyMeta(ItemStack item) {
            ItemMeta meta = item.getItemMeta();

            if (title != null) {
                meta.setDisplayName(TextUtils.formatText(title));
            }

            if (lore != null) {
                meta.setLore(TextUtils.formatText(lore));
            }

            item.setItemMeta(meta);
        }

        public List<Integer> applyPosition(int cell) {
            if (row != -1 && col != -1) {
                return Collections.singletonList(col + row * inventoryType.columns);
            }

            return positions == null ? Collections.singletonList(cell) : positions;
        }

        public int applyPositionRow(int row) {
            return row;
        }

        public int applyPositionCol(int col) {
            return col;
        }
    }

    private class MirrorFill extends CustomButton {
        private final boolean mirrorRow;
        private final boolean mirrorCol;

        public MirrorFill(String key, int row, int col, boolean mirrorRow, boolean mirrorCol, CompatibleMaterial item) {
            super(key, row, col, null, null, item);

            this.mirrorRow = mirrorRow;
            this.mirrorCol = mirrorCol;
        }

        public boolean isMirrorRow() {
            return mirrorRow;
        }

        public boolean isMirrorCol() {
            return mirrorCol;
        }
    }

    private class CustomContent {
        private final String guiKey;
        private final Map<String, CustomButton> customizedButtons = new HashMap<>();
        private final Map<String, CustomButton> customButtons = new HashMap<>();
        private final List<String> disabledButtons = new ArrayList<>();

        private int rows = -1;

        public CustomContent(String guiKey) {
            this.guiKey = guiKey;
        }

        public String getGuiKey() {
            return guiKey;
        }

        public CustomButton getCustomizedButton(String key) {
            return customizedButtons.get(key);
        }

        public CustomButton getCustomButton(String key) {
            return customizedButtons.get(key);
        }

        public Map<String, CustomButton> getCustomButtons() {
            return Collections.unmodifiableMap(customButtons);
        }

        public void addButton(String key, String position, String title, List<String> lore, CompatibleMaterial item) {
            List<Integer> positions = Arrays.stream(position.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            CustomButton customButton = new CustomButton(key, positions, title, lore, item);

            if (key.startsWith("custom_")) {
                customButtons.put(key, customButton);
                return;
            }

            customizedButtons.put(key, customButton);
        }

        public void addButton(String key, int row, int col, String title, List<String> lore, CompatibleMaterial item) {
            CustomButton customButton = new CustomButton(key, row, col, title, lore, item);

            if (key.startsWith("custom_")) {
                customButtons.put(key, customButton);
                return;
            }

            customizedButtons.put(key, customButton);
        }

        public void addButton(String key, int row, int col, boolean mirrorRow, boolean mirrorCol, CompatibleMaterial item) {
            MirrorFill mirrorFill = new MirrorFill(key, row, col, mirrorRow, mirrorCol, item);

            if (key.startsWith("custom_")) {
                customButtons.put(key, mirrorFill);
                return;
            }

            customizedButtons.put(key, mirrorFill);
        }

        public boolean isButtonCustomized(String key) {
            return customizedButtons.containsKey(key);
        }

        public void disableButton(String button) {
            disabledButtons.add(button);
        }

        public boolean isButtonDisabled(String button) {
            return disabledButtons.contains(button);
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }
    }
}
