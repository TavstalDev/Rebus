package io.github.tavstaldev.rebus.gui;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.database.IRebusDatabase;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.economy.IEconomyManager;
import io.github.tavstaldev.rebus.models.Chest;
import io.github.tavstaldev.rebus.database.models.Cooldown;
import io.github.tavstaldev.rebus.database.models.ECooldownType;
import io.github.tavstaldev.rebus.util.TimeUtil;
import io.github.tavstaldev.yggra.core.gui.GuiBase;
import io.github.tavstaldev.yggra.core.gui.GuiButton;
import io.github.tavstaldev.yggra.core.gui.GuiManager;
import io.github.tavstaldev.yggra.core.scheduler.YggraTask;
import io.github.tavstaldev.yggra.core.services.ChatService;
import io.github.tavstaldev.yggra.core.services.TranslationService;
import io.github.tavstaldev.yggra.shadow.triumpGui.guis.BaseGui;
import io.github.tavstaldev.yggra.shadow.triumpGui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Main GUI displaying available chests for purchase and preview.
 */
public class MainGUI extends GuiBase {
    private final RebusConfig config;
    private final TranslationService translator;
    private final ChatService chat;
    private final GuiManager gui;
    private final IEconomyManager economyManager;
    private final IRebusDatabase database;
    private final ChestManager chestManager;
    public final static String ID = "main";
    private final HashSet<UUID> ongoingTransactions = new HashSet<>();

    /**
     * Creates the main GUI.
     *
     * @param plugin         The plugin instance.
     * @param database       The database for cooldown and usage checks.
     * @param chestManager   The chest manager for available chests.
     * @param economyManager The economy manager for purchases.
     */
    public MainGUI(Rebus plugin, IRebusDatabase database, ChestManager chestManager, IEconomyManager economyManager) {
        super(ID, plugin, "main.yml",
                "gui.main.title",
                true,
                1,
                new LinkedHashMap<>() {{
                    put("chest_slots", new ArrayList<>() {{
                        add("0-7");
                    }});
                }},
                new LinkedHashSet<>() {{
                    // Placeholder
                    add(new GuiButton(null, "§r", false, null, false, List.of("0-8"),
                            new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
                    // Back button
                    add(new GuiButton("CLOSE","gui.common.close",
                            true, null, false, List.of("8"),
                            new ItemStack(Material.BARRIER)));
                }});
        this.config = plugin.config();
        this.translator = plugin.translator();
        this.chat = plugin.chat();
        this.gui = plugin.gui();
        this.database = database;
        this.chestManager = chestManager;
        this.economyManager = economyManager;
    }

    /**
     * Refreshes the GUI with the available chests and their purchase/preview actions.
     */
    @Override
    public void refresh(@NotNull Player player, @NotNull BaseGui sgMenu) {
        UUID playerId = player.getUniqueId();

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("chest_slots", new ArrayList<>());
        List<Chest> chests = chestManager.getChests().values().stream().toList();
        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= chests.size()) {
                // No pagination, just skip
                //sgMenu.removeButton(0, slot);
                continue;
            }

            Chest chest = chests.get(i);
            if (chest == null) {
                logger.warn("Failed to get chest.");
                continue;
            }

           var item = chest.getTranslatedDisplayItem(player, translator, true);

            sgMenu.setItem(slot, new GuiItem(item, event ->
            {
                if (event.isRightClick()) {
                    gui.open(player, PreviewGUI.ID, chest.getKey());
                    return;
                }

                // Check if the player has the required permission.
                if (!player.hasPermission(chest.getPermission())) {
                    chat.sendLocalizedMsg(player, "general.error.no-permission");
                    return;
                }

                // Prevent async thread abuse
                if (ongoingTransactions.contains(playerId)) {
                    chat.sendLocalizedMsg(player, "chests.error.transaction-ongoing");
                    return;
                }
                if (chestManager.playersUnlocking.contains(playerId)) {
                    chat.sendLocalizedMsg(player, "chests.error.buy-while-opening");
                    return;
                }

                // Check if the player's inventory has space.
                if (player.getInventory().firstEmpty() == -1) {
                    chat.sendLocalizedMsg(player, "chests.error.buy-inventory-full");
                    return;
                }

                // Check if the player has enough balance to purchase the chest.
                double balance =  economyManager.getBalance(player);
                if (balance < chest.getCost()) {
                    chat.sendLocalizedMsg(player, "general.error.not-enough-money", Map.of("balance", balance));
                    return;
                }

                plugin.scheduler().runAsync(new YggraTask() {
                    @Override
                    public void run() {
                        if (!ongoingTransactions.add(playerId)) {
                            chat.sendLocalizedMsg(player, "chests.error.transaction-ongoing");
                            return;
                        }

                        try {
                            // Check if the player is on a buy cooldown for the chest.
                            if (!player.hasPermission("rebus.bypass.buycooldown")) {
                                long remainingTime = database.getCooldown(playerId, config.storageContext, ECooldownType.BUY, chest.getKey());
                                if (remainingTime > 0) {
                                    chat.sendLocalizedMsg(player, "chests.error.buy-cooldown", Map.of("time", TimeUtil.formatDuration(translator, player, remainingTime)));
                                    return;
                                }

                                if (chest.getBuyCooldown() > 0) {
                                    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(chest.getBuyCooldown());
                                    database.cooldowns().add(new Cooldown(UUID.randomUUID(), playerId, config.storageContext, ECooldownType.BUY, chest.getKey(), expiresAt));
                                }
                            }

                            // Deduct the cost
                            if (chest.getCost() > 0)
                                economyManager.withdraw(player, chest.getCost());

                            plugin.scheduler().run(new YggraTask() {
                                @Override
                                public void run() {
                                    chestManager.giveChest(player, chest, 1);
                                    chat.sendLocalizedMsg(player, "chests.purchase-successful", Map.of(
                                                    "amount", "1",
                                                    "chest", translator.localize(player, chest.getNameKey())
                                            )
                                    );
                                }
                            });
                        }
                        finally {
                            ongoingTransactions.remove(playerId);
                        }
                    }
                });
            }));
        }
        sgMenu.open(player);
    }

    /**
     * Handles button actions (close).
     */
    @Override
    public void executeCommand(Player player, String action, InventoryClickEvent event) {
        if (action.equalsIgnoreCase("CLOSE")) {
            gui.close(player, false);
        }
    }

    /**
     * Opens the main GUI for the player.
     */
    @Override
    public void onOpen(@NotNull Player player, Object... args) {
        BaseGui menu = gui.getMenu(player, ID);
        if (menu == null)
            return;
        refresh(player, menu);
    }
}