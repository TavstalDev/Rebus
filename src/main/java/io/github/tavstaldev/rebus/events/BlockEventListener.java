package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.database.IRebusDatabase;
import io.github.tavstaldev.rebus.database.models.ChestUsage;
import io.github.tavstaldev.rebus.database.models.ECooldownType;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.models.Chest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import io.github.tavstaldev.yggra.core.scheduler.IScheduler;
import io.github.tavstaldev.yggra.core.scheduler.YggraTask;
import io.github.tavstaldev.yggra.core.services.ChatService;
import io.github.tavstaldev.yggra.core.services.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Handles block-related events such as placing and breaking blocks.
 */
public class BlockEventListener implements Listener {
    private final Rebus plugin;
    private final RebusConfig config;
    private final TranslationService translator;
    private final ChatService chat;
    private final IScheduler scheduler;
    private final IRebusDatabase database;
    private final ChestManager chestManager;
    private final HashSet<UUID> handlingPlaceEvent = new HashSet<>();

    /**
     * Creates the block event listener and registers it with the server.
     *
     * @param plugin       The plugin instance.
     * @param database     The database for usage and cooldown checks.
     * @param chestManager The chest manager for checking unlocking states.
     */
    public BlockEventListener(Rebus plugin, IRebusDatabase database, ChestManager chestManager) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.translator = plugin.translator();
        this.chat = plugin.chat();
        this.scheduler = plugin.scheduler();
        this.database = database;
        this.chestManager = chestManager;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }


    /**
     * Handles the BlockPlaceEvent, validating and processing the placement of custom chests.
     *
     * @param event The BlockPlaceEvent triggered when a player places a block.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack itemInHand = event.getItemInHand();

        // Check if the item in hand is valid and has metadata.
        if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            return;
        }

        var meta = itemInHand.getItemMeta();

        // Check if the item has the custom chest key.
        var chestKey = chestManager.getChestKey();
        if (!meta.getPersistentDataContainer().has(chestKey)) {
            return;
        }

        // Retrieve the chest key and corresponding chest object.
        String key = meta.getPersistentDataContainer().get(chestKey, PersistentDataType.STRING);
        Chest chest = chestManager.getChest(key);
        if (chest == null) {
            return;
        }

        // Cancel the event to prevent the block from being placed normally.
        event.setCancelled(true);

        // Check if the block location is already occupied by an unlocking chest.
        if (chestManager.chestsUnderUnlocking.contains(event.getBlock().getLocation())) {
            chat.sendLocalizedMsg(player, "chest.error.location-occupied");
            return;
        }

        // Check if the player is already unlocking a chest.
        if (chestManager.playersUnlocking.contains(playerId)) {
            chat.sendLocalizedMsg(player, "chests.error.already-opening");
            return;
        }

        // Check if the player has the required permission to place the chest.
        if (!player.hasPermission(chest.getPermission())) {
            chat.sendLocalizedMsg(player, "general.error.no-permission");
            return;
        }

        if (handlingPlaceEvent.contains(playerId))
            return;

        handlingPlaceEvent.add(playerId);
        scheduler.runAsync(new YggraTask() {
            @Override
            public void run() {
                synchronized (plugin.getPlayerLock(playerId)) {
                    try {
                        ChestUsage usage = database.getUsage(playerId, config.storageContext, key);
                        if (usage == null || usage.getUsages() < 1) {
                            chat.sendLocalizedMsg(player, "chests.error.no-usages-left");
                            scheduler.run(() -> handlingPlaceEvent.remove(playerId));
                            return;
                        }

                        // Check if the chest is on cooldown for the player.
                        long remainingTime = database.getCooldown(playerId, config.storageContext, ECooldownType.OPEN, key);
                        if (remainingTime > 0 && !player.hasPermission("rebus.bypass.cooldown")) {
                            chat.sendLocalizedMsg(player, "chests.open-cooldown", Map.of("time", TimeUtil.formatDuration(translator, player, remainingTime)));
                            scheduler.run(() -> handlingPlaceEvent.remove(playerId));
                            return;
                        }

                        // Handle the placement of the chest.
                        scheduler.run(new YggraTask() {
                            @Override
                            public void run() {
                                try {
                                    chestManager.handlePlaceChest(player, chest, itemInHand, event.getBlock());
                                } finally {
                                    handlingPlaceEvent.remove(playerId);
                                }
                            }
                        });
                    } catch (Exception ex) {
                        scheduler.run(() -> handlingPlaceEvent.remove(playerId));
                    }
                }
            }
        });
    }

    /**
     * Handles the BlockBreakEvent, preventing the breaking of blocks that are under unlocking.
     *
     * @param event The BlockBreakEvent triggered when a player breaks a block.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        // Cancel the event if the block is under unlocking.
        if (chestManager.chestsUnderUnlocking.contains(event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    /**
     * Prevents pistons from pushing blocks that are currently being unlocked.
     *
     * @param event The piston extend event.
     */
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (chestManager.chestsUnderUnlocking.contains(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents pistons from pulling blocks that are currently being unlocked.
     *
     * @param event The piston retract event.
     */
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (chestManager.chestsUnderUnlocking.contains(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents explosions from destroying blocks that are currently being unlocked.
     *
     * @param event The block explosion event.
     */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
                chestManager.chestsUnderUnlocking.contains(block.getLocation())
        );
    }
}