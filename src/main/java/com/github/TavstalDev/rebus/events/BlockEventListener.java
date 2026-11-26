package com.github.TavstalDev.rebus.events;

import com.cryptomorin.xseries.XMaterial;
import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import com.github.TavstalDev.rebus.util.TimeUtil;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.UUID;

/**
 * Handles block-related events such as placing and breaking blocks.
 */
public class BlockEventListener implements Listener {
    private final Rebus plugin;
    private final RebusConfig config;
    private final ITranslationService translator;
    private final IChatService chat;
    private final IScheduler scheduler;
    private final IRebusDatabase database;
    private final IBoxService boxService;
    private final INbtAccessor nbtAccessor;
    private final HashSet<UUID> handlingPlaceEvent = new HashSet<>();

    public BlockEventListener(Rebus plugin, ITranslationService translator,
                              IChatService chatService, IScheduler scheduler,
                              IRebusDatabase database, IBoxService boxService,
                              INbtAccessor nbtAccessor) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.translator = translator;
        this.chat = chatService;
        this.scheduler = scheduler;
        this.database = database;
        this.boxService = boxService;
        this.nbtAccessor = nbtAccessor;
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

        if (XMaterial.AIR.isSimilar(itemInHand))
            return;

        // Check if the item has the custom chest key.
        String chestKey = boxService.getChestKey();
        String key = nbtAccessor.getOrDefault(itemInHand, chestKey, "");
        if (key == null || key.isEmpty())
            return;

        Chest chest = boxService.getChest(key);
        if (chest == null)
            return;

        // Cancel the event to prevent the block from being placed normally.
        event.setCancelled(true);

        // Check if the block location is already occupied by an unlocking chest.
        if (boxService.occupiedLocations().contains(event.getBlock().getLocation())) {
            chat.sendLocalizedMsg(player, "chests.error.location-occupied");
            return;
        }

        // Check if the player is already unlocking a chest.
        if (boxService.playersUnlocking().contains(playerId)) {
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
                        ChestUsage usage = database.getUsage(playerId, config.storage.getContext(), key);
                        if (usage == null || usage.getUsages() < 1) {
                            chat.sendLocalizedMsg(player, "chests.error.no-usages-left");
                            scheduler.run(() -> handlingPlaceEvent.remove(playerId));
                            return;
                        }

                        // Check if the chest is on cooldown for the player.
                        long remainingTime = database.getCooldown(playerId, config.storage.getContext(), ECooldownType.OPEN, key);
                        if (remainingTime > 0 && !player.hasPermission("rebus.bypass.cooldown")) {
                            chat.sendLocalizedMsg(player, "chests.error.use-cooldown", MapUtil.of("time", TimeUtil.formatDuration(translator, player, remainingTime)));
                            scheduler.run(() -> handlingPlaceEvent.remove(playerId));
                            return;
                        }

                        // Handle the placement of the chest.
                        scheduler.run(new YggraTask() {
                            @Override
                            public void run() {
                                try {
                                    boxService.handlePlaceChest(player, chest, itemInHand, event.getBlock());
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
        if (boxService.occupiedLocations().contains(event.getBlock().getLocation()))
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
            if (boxService.occupiedLocations().contains(block.getLocation())) {
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
            if (boxService.occupiedLocations().contains(block.getLocation())) {
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
                boxService.occupiedLocations().contains(block.getLocation())
        );
    }
}