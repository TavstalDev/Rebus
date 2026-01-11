package io.github.tavstaldev.rebus.gui;

import io.github.tavstaldev.minecorelib.managers.MenuManager;
import io.github.tavstaldev.minecorelib.models.gui.MenuBase;
import io.github.tavstaldev.minecorelib.models.gui.MenuButton;
import io.github.tavstaldev.minecorelib.shadow.spigui.buttons.SGButton;
import io.github.tavstaldev.minecorelib.shadow.spigui.menu.SGMenu;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.minecorelib.utils.GuiUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.managers.economy.IEconomyManager;
import io.github.tavstaldev.rebus.models.ECooldownType;
import io.github.tavstaldev.rebus.models.PlayerCache;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainGUI extends MenuBase {

    public static String ID = "main";

    public MainGUI() {
        super(Rebus.Instance, "main.yml");
    }

    @Override
    protected void loadDefaults() {
        menuTitle = resolveGet("title", "GUI.Title");
        isMenuTitleTranslated = resolveGet("title_translated", true);
        menuSize = resolveGet("size", 1);
        dynamicSlots = resolveDynamicSlots(new LinkedHashMap<>() {{
            put("chest_slots", new ArrayList<>() {{
                add("0-7");
            }});
        }});
        menuButtons = resolveButtons(new LinkedHashSet<>() {{
            // Placeholder
            add(new MenuButton(Material.BLACK_STAINED_GLASS_PANE, null, 1, "§r", null, null, null, null, List.of("0-8"), null));
            // Back button
            add(new MenuButton(Material.SPRUCE_DOOR, null, 1, null, "GUI.Close", null, null, 8, null,  List.of("[CLOSE]")));
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

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("chest_slots", new ArrayList<>());
        List<RebusChest> chests = new ArrayList<>(Rebus.chestManager().getChests());
        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= chests.size()) {
                sgMenu.removeButton(0, slot);
                continue;
            }

            RebusChest chest = chests.get(i);
            if (chest== null) {
                logger.warn("Failed to get chest.");
                continue;
            }

            List<Component> lore = new ArrayList<>();
            String price = Rebus.translator().localize("GUI.Price", Map.of("price", chest.getCost()));
            lore.add(ChatUtils.translateColors(price, true));
            for (String line : chest.getDescription()) {
                lore.add(ChatUtils.translateColors(line, true));
            }
            lore.add(Component.text(""));
            if (!player.hasPermission(chest.getPermission())) {
                lore.add(ChatUtils.translateColors(Rebus.translator().localize("GUI.NoPermission"), true));
            }
            else {
                lore.add(ChatUtils.translateColors(Rebus.translator().localize("GUI.ClickToBuy"), true));
            }
            lore.add(ChatUtils.translateColors(Rebus.translator().localize("GUI.ClickToPreview"), true));

            // Create an item representing the chest and add it to the GUI.
            ItemStack item = GuiUtils.createItem(
                    Rebus.Instance,
                    chest.getMaterial(),
                    chest.getName(),
                    lore
            );

            sgMenu.setButton(0, slot, new SGButton(item).withListener(event ->
            {
                if (event.isRightClick()) {
                    MenuManager manager = plugin.getMenuManager();
                    if (manager != null) {
                        playerCache.setPreviewChest(chest);
                        manager.open(player, PreviewGUI.ID);
                    }
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
                IEconomyManager economyManager = Rebus.economyManager();
                double balance =  economyManager.getBalance(player);
                if (balance < chest.getCost()) {
                    Rebus.Instance.sendLocalizedMsg(player, "General.NotEnoughMoney", Map.of("balance", balance));
                    return;
                }

                // Check if the chest is on cooldown for the player.
                long remainingTime = Rebus.database().getCooldown(playerId, ECooldownType.OPEN, chest.getKey());
                if (remainingTime > 0 && !player.hasPermission("rebus.bypass.cooldown")) {
                    Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                    return;
                }

                // Check if the player is on a buy cooldown for the chest.
                remainingTime = Rebus.database().getCooldown(playerId, ECooldownType.BUY, chest.getKey());
                if (remainingTime > 0 && !player.hasPermission("rebus.bypass.buycooldown")) {
                    Rebus.Instance.sendLocalizedMsg(player, "Chests.BuyCooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
                    return;
                }

                // Deduct the cost and give the chest to the player.
                if (chest.getCost() > 0)
                    economyManager.withdraw(player, chest.getCost());
                if (chest.getBuyCooldown() > 0)
                    Rebus.database().addCooldown(playerId, ECooldownType.BUY, chest.getKey(), chest.getBuyCooldown());
                chest.give(player, 1);
                Rebus.Instance.sendLocalizedMsg(player, "General.PurchaseSuccessful");
            }));
        }
        player.openInventory(sgMenu.getInventory());
    }

    @Override
    public void executeCommand(@NotNull Player player, @NotNull String command) {
        String[] parts = command.split("\\s+");
        switch (parts[0].toLowerCase()) {
            case "[close]" -> {
                MenuManager manager = plugin.getMenuManager();
                if (manager != null)
                    manager.close(player, false);
            }
        }
    }

    @Override
    public void onOpen(@NotNull Player player) {
        MenuManager manager = plugin.getMenuManager();
        if (manager != null) {
            SGMenu menu = manager.getMenu(player, ID);
            if (menu != null)
                refresh(player, menu);
        }
    }
}