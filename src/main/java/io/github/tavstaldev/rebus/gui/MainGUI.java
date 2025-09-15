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
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import io.github.tavstaldev.rebus.util.PermissionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainGUI {
    private static final PluginLogger _logger = Rebus.Logger().WithModule(MainGUI.class);
    private static final PluginTranslator _translator = Rebus.Instance.getTranslator();

    public static SGMenu create(@NotNull Player player) {
        try {
            RebusConfig config = Rebus.Config();
            int rows = config.guiRows;
            SGMenu menu = Rebus.GUI().create(_translator.Localize(player, "GUI.Title"), rows);

            // Create Placeholders
            if (config.guiFillEmptySlots) {
                SGButton placeholderButton = new SGButton(GuiUtils.createItem(Rebus.Instance, config.guiPlaceholderMaterial, " "));
                int slots = rows * 9;
                for (int i = 0; i < slots; i++) {
                    menu.setButton(0, i, placeholderButton);
                }
            }

            // Close Button
            SGButton closeButton = new SGButton(
                    GuiUtils.createItem(Rebus.Instance, config.guiCloseMaterial, _translator.Localize(player, "GUI.Close"))
            ).withListener(event -> close(player));
            menu.setButton(0, config.guiCloseBtnSlot, closeButton);


            return menu;
        }
        catch (Exception ex) {
            _logger.Error("An error occurred while creating the main GUI.");
            _logger.Error(ex);
            return null;
        }
    }

    public static void open(@NotNull Player player) {
        var playerCache = PlayerCacheManager.get(player.getUniqueId());
        // Show the GUI
        playerCache.setGuiOpened(true);
        player.openInventory(playerCache.getMainMenu().getInventory());
        refresh(player);
    }

    public static void close(@NotNull Player player) {
        var playerCache = PlayerCacheManager.get(player.getUniqueId());
        player.closeInventory();
        playerCache.setGuiOpened(false);
    }

    public static void refresh(@NotNull Player player) {
        try {
            var playerId = player.getUniqueId();
            var playerCache = PlayerCacheManager.get(playerId);
            var menu = playerCache.getMainMenu();

            // Daily Quests
            var chests =  Rebus.ChestManager().getChests();
            for (RebusChest chest : chests) {
                List<Component> lore = new ArrayList<>();
                String price = Rebus.Translator().Localize("GUI.Price", Map.of("price", chest.getCost()));
                lore.add(ChatUtils.translateColors(price, true));
                for (String line : chest.getDescription()) {
                    lore.add(ChatUtils.translateColors(line, true));
                }
                if (!PermissionUtils.checkPermission(player, chest.getPermission())) {
                    lore.add(ChatUtils.translateColors(Rebus.Translator().Localize("GUI.NoPermission"), true));
                }

                ItemStack item = GuiUtils.createItem(
                        Rebus.Instance,
                        chest.getMaterial(),
                        chest.getName(),
                        lore
                );

                SGButton chestButton = new SGButton(item).withListener(event -> {
                    if (event.isRightClick()) {
                        // TODO: Add preview
                    }

                    if (!PermissionUtils.checkPermission(player, chest.getPermission())) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return;
                    }

                    var balance = Rebus.BanyaszApi().getBalance(playerId);
                    if (balance < chest.getCost()) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NotEnoughMoney", Map.of("balance", balance));
                        return;
                    }

                    long remainingTime = playerCache.getCooldown(chest);
                    if (remainingTime > 0 && !PermissionUtils.checkPermission(player, "rebus.bypass.cooldown")) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                        return;
                    }

                    remainingTime = playerCache.getBuyCooldown(chest);
                    if (remainingTime > 0 && !PermissionUtils.checkPermission(player, "rebus.bypass.buycooldown")) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.BuyCooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                        return;
                    }

                    if (chest.getCost() > 0)
                        Rebus.BanyaszApi().decreaseBalance(player.getUniqueId(), (int)chest.getCost());
                    chest.give(player, 1);
                    playerCache.addBuyCooldown(chest);
                    Rebus.Instance.sendLocalizedMsg(player, "General.PurchaseSuccessful");
                });

                menu.setButton(0, chest.getSlot(), chestButton);
            }
            player.openInventory(menu.getInventory());
        }
        catch (Exception ex) {
            _logger.Error("An error occurred while refreshing the main GUI.");
            _logger.Error(ex);
        }
    }
}
