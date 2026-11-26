package com.github.TavstalDev.rebus.gui;

import com.cryptomorin.xseries.XMaterial;
import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.database.models.Cooldown;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import com.github.TavstalDev.rebus.services.economy.IEconomyService;
import com.github.TavstalDev.rebus.util.TimeUtil;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.api.utils.ListUtil;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.gui.GuiBase;
import com.github.TavstalDev.yggraLib.bukkit.gui.GuiButton;
import com.github.TavstalDev.yggraLib.bukkit.gui.IGuiService;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
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
    private final ITranslationService translator;
    private final IChatService chat;
    private final IScheduler scheduler;
    private final IItemStackAccessor itemStackAccessor;
    private final INbtAccessor nbtAccessor;
    private final IGuiService gui;
    private final IEconomyService economyManager;
    private final IRebusDatabase database;
    private final IBoxService boxService;
    public final static String ID = "main";
    private final HashSet<UUID> ongoingTransactions = new HashSet<>();

    public MainGUI(Rebus plugin, ITranslationService translator, IChatService chatService, IScheduler scheduler,
                   IItemStackAccessor itemStackAccessor, INbtAccessor nbtAccessor, IGuiService guiService,
                   IEconomyService economyService, IRebusDatabase database, IBoxService boxService) {
        super(ID, plugin, "main.yml",
                "gui.main.title",
                true,
                1,
                new LinkedHashMap<String, List<String>>() {{
                    put("chest_slots", new ArrayList<String>() {{
                        add("0-7");
                    }});
                }},
                new LinkedHashSet<GuiButton>() {{
                    // Placeholder
                    add(new GuiButton(null, "§r ", false, null, false, ListUtil.of("0-8"),
                            XMaterial.BLACK_STAINED_GLASS_PANE.parseItem()));
                    // Back button
                    add(new GuiButton("CLOSE","gui.common.close",
                            true, null, false, ListUtil.of("8"),
                            XMaterial.BARRIER.parseItem()));
                }});
        this.config = plugin.config();
        this.translator = translator;
        this.chat = chatService;
        this.scheduler = scheduler;
        this.itemStackAccessor = itemStackAccessor;
        this.nbtAccessor = nbtAccessor;
        this.gui = guiService;
        this.database = database;
        this.boxService = boxService;
        this.economyManager = economyService;
    }

    /**
     * Refreshes the GUI with the available chests and their purchase/preview actions.
     */
    @Override
    public void refresh(@NotNull Player player, @NotNull BaseGui menu) {
        UUID playerId = player.getUniqueId();

        // Handle dynamic slots
        List<Integer> dynamicSlots = this.dynamicSlots.getOrDefault("chest_slots", new ArrayList<>());
        List<Chest> chests = new ArrayList<>(boxService.getChests().values());
        for (int i = 0; i < dynamicSlots.size(); i++) {
            int slot = dynamicSlots.get(i);

            if (i >= chests.size()) {
                continue;
            }

            Chest chest = chests.get(i);
            if (chest == null) {
                logger.warn("Failed to get chest.");
                continue;
            }

           ItemStack item = chest.getTranslatedDisplayItem(player, translator, itemStackAccessor, nbtAccessor, true);

            menu.setItem(slot, new GuiItem(item, event ->
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
                if (boxService.playersUnlocking().contains(playerId)) {
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
                    chat.sendLocalizedMsg(player, "general.error.not-enough-money", MapUtil.of("balance", balance));
                    return;
                }

                scheduler.runAsync(new YggraTask() {
                    @Override
                    public void run() {
                        if (!ongoingTransactions.add(playerId)) {
                            chat.sendLocalizedMsg(player, "chests.error.transaction-ongoing");
                            return;
                        }

                        try {
                            // Check if the player is on a buy cooldown for the chest.
                            if (!player.hasPermission("rebus.bypass.buycooldown")) {
                                long remainingTime = database.getCooldown(playerId, config.storage.getContext(), ECooldownType.BUY, chest.getKey());
                                if (remainingTime > 0) {
                                    chat.sendLocalizedMsg(player, "chests.error.buy-cooldown", MapUtil.of("time", TimeUtil.formatDuration(translator, player, remainingTime)));
                                    return;
                                }

                                if (chest.getBuyCooldown() > 0) {
                                    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(chest.getBuyCooldown());
                                    database.cooldowns().add(new Cooldown(UUID.randomUUID(), playerId, config.storage.getContext(), ECooldownType.BUY, chest.getKey(), expiresAt));
                                }
                            }

                            // Deduct the cost
                            if (chest.getCost() > 0)
                                economyManager.withdraw(player, chest.getCost());

                            scheduler.run(new YggraTask() {
                                @Override
                                public void run() {
                                    boxService.giveChest(player, chest, 1);
                                    chat.sendLocalizedMsg(player, "chests.purchase-successful", MapUtil.of(
                                                    "amount", "1",
                                                    "chest", translator.localize(playerId, chest.getNameKey())
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
        menu.open(player);
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