package io.github.tavstaldev.rebus.gui;

import io.github.tavstaldev.minecorelib.core.GuiDupeDetector;
import io.github.tavstaldev.minecorelib.managers.MenuManager;
import io.github.tavstaldev.minecorelib.models.gui.MenuBase;
import io.github.tavstaldev.minecorelib.models.gui.MenuButton;
import io.github.tavstaldev.minecorelib.shadow.spigui.buttons.SGButton;
import io.github.tavstaldev.minecorelib.shadow.spigui.menu.SGMenu;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.PlayerCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PreviewGUI extends MenuBase {

    public static String ID = "preview";

    public PreviewGUI() {
        super(Rebus.Instance, "preview.yml");
    }

    @Override
    protected void loadDefaults() {
        menuTitle = resolveGet("title", "GUI.PreviewTitle");
        isMenuTitleTranslated = resolveGet("title_translated", true);
        menuSize = resolveGet("size", 6);
        dynamicSlots = resolveDynamicSlots(new LinkedHashMap<>() {{
            put("item_slots", new ArrayList<>() {{
                add("10-16");
                add("19-25");
                add("28-34");
                add("37-43");
            }});
        }});
        menuButtons = resolveButtons(new LinkedHashSet<>() {{
            // Placeholder
            add(new MenuButton(Material.BLACK_STAINED_GLASS_PANE, null, 1, "§r", null, null, null, null, List.of("0-9", "17-18", "26-27", "35-36", "44", "46-47", "51-53"), null));
            // Back button
            add(new MenuButton(Material.SPRUCE_DOOR, null, 1, null, "GUI.Back", null, null, 45, null,  List.of("[OPEN] " + MainGUI.ID)));
            // Previous button
            add(new MenuButton(Material.ARROW, null, 1, null, "GUI.PreviousPage", null, null, 48, null, List.of("[PREV_PAGE]")));
            // Page button, NOTE: should be updated on refresh
            add(new MenuButton(Material.PAPER, null, 1, "{PAGE}", null, null, null, 49, null, null));
            // Next button
            add(new MenuButton(Material.ARROW, null, 1, null, "GUI.NextPage", null, null, 50, null, List.of("[NEXT_PAGE]")));
        }});
    }

    @Override
    public SGMenu create(@NotNull Player player) {
        MenuManager menuManager = plugin.getMenuManager();
        if (menuManager == null)
            throw new RuntimeException("Menu manager was not initialized.");
        SGMenu menu = menuManager.getSpiGUI().create(isMenuTitleTranslated ? translator.localize(player, menuTitle) : menuTitle, menuSize);

        for (MenuButton button : menuButtons) {
            button.apply(player, translator, menu, this);
        }
        return menu;
    }

    @Override
    public void refresh(@NotNull Player player, @NotNull SGMenu sgMenu) {
        UUID playerId = player.getUniqueId();
        PlayerCache playerCache = PlayerCacheManager.get(playerId);

        // 1. Find page button
        MenuButton pageButton = null;
        for (MenuButton btn : menuButtons) {
            if (btn.getTitle() != null && btn.getTitle().equalsIgnoreCase("{PAGE}")) {
                pageButton = btn;
                break;
            }
        }

        // 2. Update page button
        if (pageButton != null) {
            String pageText = translator.localize(player,  "GUI.Page", Map.of(
                    "page", String.valueOf(playerCache.getPreviewPage()) // Localize the page number
            ));
            Component pageComp = ChatUtils.translateColors(pageText, true);

            for (Integer slot : pageButton.getSlots()) {
                SGButton btn = sgMenu.getButton(0, slot);
                if (btn == null)
                    continue;

                ItemStack icon = btn.getIcon();
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.displayName(pageComp);
                    icon.setItemMeta(meta);
                }
                btn.setIcon(icon);
            }
        }

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("item_slots", new ArrayList<>());
        Map<ItemStack, Double> rewards = playerCache.getPreviewChest().getItemChances();
        List<ItemStack> pagedRewards = new ArrayList<>(rewards.keySet());
        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= rewards.size()) {
                sgMenu.removeButton(0, slot);
                continue;
            }

            ItemStack item = pagedRewards.get(i).clone();
            Double chance = rewards.get(item);
            ItemMeta meta = item.getItemMeta();
            List<Component> lore;
            if (meta.hasLore())
                lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));
            else
                lore = new ArrayList<>();
            lore.add(Component.text(""));
            String chanceText = translator.localize(player, "GUI.Chance", Map.of("chance", String.format("%.2f", chance * 100)));
            lore.add(ChatUtils.translateColors(chanceText, true));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(GuiDupeDetector.getDupeProtectedKey(), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            sgMenu.setButton(0, slot, new SGButton(item));
        }
        player.openInventory(sgMenu.getInventory());
    }

    @Override
    public void executeCommand(@NotNull Player player, @NotNull String command) {
        String[] parts = command.split("\\s+");
        switch (parts[0].toLowerCase()) {
            case "[next_page]" -> {
                PlayerCache playerData = PlayerCacheManager.get(player.getUniqueId());
                int maxPage = 1 + (playerData.getPreviewChest().getPossibleItems().size() / dynamicSlots.getOrDefault("category_slots", new ArrayList<>()).size());
                if (playerData.getPreviewPage() + 1 > maxPage)
                    return;
                playerData.setPreviewPage(playerData.getPreviewPage() + 1);

                MenuManager manager = plugin.getMenuManager();
                if (manager == null)
                    break;
                SGMenu menu = manager.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
            }
            case "[prev_page]" -> {
                PlayerCache playerData = PlayerCacheManager.get(player.getUniqueId());
                if (playerData.getPreviewPage() - 1 <= 0)
                    return;
                playerData.setPreviewPage(playerData.getPreviewPage() - 1);

                MenuManager manager = plugin.getMenuManager();
                if (manager == null)
                    break;
                SGMenu menu = manager.getMenu(player, ID);
                if (menu == null)
                    break;
                refresh(player, menu);
            }
            case "[close]" -> {
                MenuManager manager = plugin.getMenuManager();
                if (manager != null)
                    manager.close(player, false);
            }
            case "[open]" -> {
                if (parts.length < 2)
                    return;
                String menuId = parts[1];
                MenuManager manager = plugin.getMenuManager();
                if (manager != null)
                    manager.open(player, menuId);
            }
        }
    }

    @Override
    public void onOpen(@NotNull Player player) {
        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
        cache.setPreviewPage(1);
        MenuManager manager = plugin.getMenuManager();
        if (manager != null) {
            SGMenu menu = manager.getMenu(player, ID);
            if (menu != null) {
                if (isMenuTitleTranslated) {
                    menu.setName(translator.localize(player, menuTitle, Map.of("chest", cache.getPreviewChest().getName())));
                }
                refresh(player, menu);
            }
        }
    }
}