package io.github.tavstaldev.rebus.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.minecorelib.core.GuiDupeDetector;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.utils.GuiUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.PlayerCache;
import io.github.tavstaldev.rebus.models.RebusChest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

public class PreviewGUI {
    private static final PluginLogger _logger = Rebus.logger().withModule(PreviewGUI.class);
    private static final Integer[] SlotPlaceholders = {
            0,  1,  2,  3,  4,  5,  6,  7,  8,
            9,                              17,
            18,                             26,
            27,                             35,
            36,                             44,
                46, 47,             51, 52, 53
    };

    /**
     * Creates the Preview GUI for the specified player.
     *
     * @param player The player for whom the GUI is being created.
     * @return The created SGMenu instance.
     */
    public static SGMenu create(@NotNull Player player) {
        try {
            SGMenu menu = Rebus.gui().create("...", 6);

            // Create Placeholders
            SGButton placeholderButton = new SGButton(GuiUtils.createItem(Rebus.Instance, Material.BLACK_STAINED_GLASS_PANE, " "));
            for (Integer slot : SlotPlaceholders) {
                menu.setButton(0, slot, placeholderButton);
            }

            // Close Button
            SGButton closeButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, Material.SPRUCE_DOOR, Rebus.Instance.localize(player, "GUI.Back")))
                    .withListener((InventoryClickEvent event) -> MainGUI.open(player));
            menu.setButton(0, 45, closeButton);

            // Previous Page Button
            SGButton prevPageButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, Material.ARROW, Rebus.Instance.localize(player, "GUI.PreviousPage")))
                    .withListener((InventoryClickEvent event) -> {
                        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
                        if (cache.getPreviewPage() - 1 <= 0)
                            return;
                        cache.setPreviewPage(cache.getPreviewPage() - 1);
                        refresh(player);
                    });
            menu.setButton(0, 48, prevPageButton);

            // Page Indicator
            SGButton pageButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, Material.PAPER, Rebus.Instance.localize(player, "GUI.Page", Map.of("page", "1")))
            );
            menu.setButton(0, 49, pageButton);

            // Next Page Button
            SGButton nextPageButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, Material.ARROW, Rebus.Instance.localize(player, "GUI.NextPage")))
                    .withListener((InventoryClickEvent event) -> {
                        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
                        int maxPage = 1 + (cache.getPreviewChest().getRewards().size() / 28);
                        if (cache.getPreviewPage() + 1 > maxPage)
                            return;
                        cache.setPreviewPage(cache.getPreviewPage() + 1);
                        refresh(player);
                    });
            menu.setButton(0, 50, nextPageButton);
            return menu;
        }
        catch (Exception ex) {
            _logger.error("An error occurred while creating the Preview GUI.");
            _logger.error(ex);
            return null;
        }
    }

    public static void open(@NotNull Player player, RebusChest chest) {
        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
        // Show the GUI
        cache.setPreviewChest(chest);
        cache.setGuiOpened(true);
        cache.setPreviewPage(1);
        cache.getPreviewMenu().setName(Rebus.Instance.localize(player, "GUI.PreviewTitle", Map.of("chest", chest.getName())));
        player.openInventory(cache.getPreviewMenu().getInventory());
        refresh(player);
    }

    /**
     * Closes the Preview GUI for the specified player.
     *
     * @param player The player for whom the GUI is being closed.
     */
    public static void close(@NotNull Player player) {
        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
        player.closeInventory();
        cache.setGuiOpened(false);
    }

    /**
     * Refreshes the Preview GUI for the specified player.
     *
     * @param player The player for whom the GUI is being refreshed.
     */
    public static void refresh(@NotNull Player player) {
        try {
            PlayerCache cache =PlayerCacheManager.get(player.getUniqueId());
            SGButton pageButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance,
                            Material.PAPER,
                            Rebus.Instance.localize(player, "GUI.Page",
                                    Map.of("page", String.valueOf(cache.getPreviewPage()))
                            )
                    )
            );
            cache.getPreviewMenu().setButton(0, 49, pageButton);

            var rewards = new ArrayList<>(cache.getPreviewChest().getPossibleItems());
            int page = cache.getPreviewPage();
            for (int i = 0; i < 28; i++) {
                int index = i + (page - 1) * 28;
                int slot = i + 10 + (2 * (i / 7));
                if (index >= rewards.size()) {
                    cache.getPreviewMenu().removeButton(0, slot);
                    continue;
                }

                ItemStack itemStack = rewards.get(index).clone();
                var meta = itemStack.getItemMeta();
                meta.getPersistentDataContainer().set(GuiDupeDetector.getDupeProtectedKey(), PersistentDataType.BOOLEAN, true);
                itemStack.setItemMeta(meta);

                cache.getPreviewMenu().setButton(0, slot, new SGButton(itemStack));
            }
            player.openInventory(cache.getPreviewMenu().getInventory());
        }
        catch (Exception ex) {
            _logger.error("An error occurred while refreshing the Preview GUI.");
            _logger.error(ex);
        }
    }
}