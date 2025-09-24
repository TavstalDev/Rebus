package io.github.tavstaldev.rebus.gui;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.core.PluginTranslator;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.minecorelib.utils.GuiUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.ECooldownType;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the main GUI for the Rebus plugin, providing methods to create, open, close, and refresh the GUI.
 */
public class MainGUI {
    // Logger instance for logging errors and information related to the MainGUI.
    private static final PluginLogger _logger = Rebus.Logger().WithModule(MainGUI.class);

    // Translator instance for localizing messages and GUI elements.
    private static final PluginTranslator _translator = Rebus.Instance.getTranslator();

    /**
     * Creates the main GUI for the specified player.
     *
     * @param player The player for whom the GUI is being created.
     * @return The created SGMenu object, or null if an error occurs.
     */
    public static SGMenu create(@NotNull Player player) {
        try {
            RebusConfig config = Rebus.Config();
            int rows = config.guiRows;
            SGMenu menu = Rebus.GUI().create(_translator.Localize(player, "GUI.Title"), rows);

            // Fill empty slots with placeholders if enabled in the configuration.
            if (config.guiFillEmptySlots) {
                SGButton placeholderButton = new SGButton(GuiUtils.createItem(Rebus.Instance, config.guiPlaceholderMaterial, " "));
                int slots = rows * 9;
                for (int i = 0; i < slots; i++) {
                    menu.setButton(0, i, placeholderButton);
                }
            }

            // Add a close button to the GUI.
            SGButton closeButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, config.guiCloseMaterial, _translator.Localize(player, "GUI.Close"))
            ).withListener(event -> close(player));
            menu.setButton(0, config.guiCloseBtnSlot, closeButton);

            return menu;
        } catch (Exception ex) {
            // Log an error if GUI creation fails.
            _logger.Error("An error occurred while creating the main GUI.");
            _logger.Error(ex);
            return null;
        }
    }

    /**
     * Opens the main GUI for the specified player.
     *
     * @param player The player for whom the GUI is being opened.
     */
    public static void open(@NotNull Player player) {
        var playerCache = PlayerCacheManager.get(player.getUniqueId());
        // Mark the GUI as opened and display it to the player.
        playerCache.setGuiOpened(true);
        player.openInventory(playerCache.getMainMenu().getInventory());
        refresh(player);
    }

    /**
     * Closes the main GUI for the specified player.
     *
     * @param player The player for whom the GUI is being closed.
     */
    public static void close(@NotNull Player player) {
        var playerCache = PlayerCacheManager.get(player.getUniqueId());
        player.closeInventory();
        playerCache.setGuiOpened(false);
    }

    /**
     * Refreshes the main GUI for the specified player, updating its contents.
     *
     * @param player The player for whom the GUI is being refreshed.
     */
    public static void refresh(@NotNull Player player) {
        try {
            var playerId = player.getUniqueId();
            var playerCache = PlayerCacheManager.get(playerId);
            var menu = playerCache.getMainMenu();

            // Populate the GUI with daily quests (chests).
            var chests = Rebus.ChestManager().getChests();
            for (RebusChest chest : chests) {
                List<Component> lore = new ArrayList<>();
                String price = Rebus.Translator().Localize("GUI.Price", Map.of("price", chest.getCost()));
                lore.add(ChatUtils.translateColors(price, true));
                for (String line : chest.getDescription()) {
                    lore.add(ChatUtils.translateColors(line, true));
                }
                lore.add(Component.text(""));
                if (!player.hasPermission(chest.getPermission())) {
                    lore.add(ChatUtils.translateColors(Rebus.Translator().Localize("GUI.NoPermission"), true));
                }
                else {
                    lore.add(ChatUtils.translateColors(Rebus.Translator().Localize("GUI.ClickToBuy"), true));
                }
                lore.add(ChatUtils.translateColors(Rebus.Translator().Localize("GUI.ClickToPreview"), true));

                // Create an item representing the chest and add it to the GUI.
                ItemStack item = GuiUtils.createItem(
                        Rebus.Instance,
                        chest.getMaterial(),
                        chest.getName(),
                        lore
                );

                SGButton chestButton = new SGButton(item).withListener(event -> {
                    if (event.isRightClick()) {
                        PreviewGUI.open(player, chest);
                        return;
                    }

                    // Check if the player has the required permission.
                    if (!player.hasPermission(chest.getPermission())) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return;
                    }

                    // Check if the player's inventory has space.
                    if (player.getInventory().firstEmpty() == -1) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.CannotBuy");
                        return;
                    }

                    // Check if the player has enough balance to purchase the chest.
                    var balance = Rebus.BanyaszApi().getBalance(playerId);
                    if (balance < chest.getCost()) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NotEnoughMoney", Map.of("balance", balance));
                        return;
                    }

                    // Check if the chest is on cooldown for the player.
                    long remainingTime = Rebus.Database().getCooldown(playerId, ECooldownType.OPEN, chest.getKey());
                    if (remainingTime > 0 && !player.hasPermission("rebus.bypass.cooldown")) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                        return;
                    }

                    // Check if the player is on a buy cooldown for the chest.
                    remainingTime = Rebus.Database().getCooldown(playerId, ECooldownType.BUY, chest.getKey());
                    if (remainingTime > 0 && !player.hasPermission("rebus.bypass.buycooldown")) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.BuyCooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                        return;
                    }

                    // Deduct the cost and give the chest to the player.
                    if (chest.getCost() > 0)
                        Rebus.BanyaszApi().decreaseBalance(playerId, (int) chest.getCost());
                    if (chest.getBuyCooldown() > 0)
                        Rebus.Database().addCooldown(playerId, ECooldownType.BUY, chest.getKey(), chest.getBuyCooldown());
                    chest.give(player, 1);
                    Rebus.Instance.sendLocalizedMsg(player, "General.PurchaseSuccessful");
                });

                menu.setButton(0, chest.getSlot(), chestButton);
            }
            // Open the updated GUI for the player.
            player.openInventory(menu.getInventory());
        } catch (Exception ex) {
            // Log an error if refreshing the GUI fails.
            _logger.Error("An error occurred while refreshing the main GUI.");
            _logger.Error(ex);
        }
    }
}