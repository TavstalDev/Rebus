package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.ECooldownType;
import io.github.tavstaldev.rebus.models.PlayerCache;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * The BlockEventListener class handles block-related events such as placing and breaking blocks.
 * It ensures that specific rules and conditions are applied when interacting with custom chests.
 */
public class BlockEventListener implements Listener {

    /**
     * Initializes the BlockEventListener by registering it with the Bukkit plugin manager.
     */
    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new BlockEventListener(), Rebus.Instance);
    }

    /**
     * Handles the BlockPlaceEvent to manage the placement of custom chests.
     *
     * @param event The BlockPlaceEvent triggered when a player places a block.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        // Check if the item in hand is valid and has metadata.
        if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            return;
        }

        var meta = itemInHand.getItemMeta();

        // Check if the item has the custom chest key.
        if (!meta.getPersistentDataContainer().has(Rebus.chestManager().getChestKey())) {
            return;
        }
        String key = meta.getPersistentDataContainer().get(Rebus.chestManager().getChestKey(), PersistentDataType.STRING);

        // Retrieve the chest associated with the key.
        RebusChest chest = Rebus.chestManager().getByKey(key);
        if (chest == null) {
            return;
        }

        // Cancel the event to prevent the default block placement.
        event.setCancelled(true);

        // Check if the block location is already occupied by another chest.
        if (Rebus.chestManager().chestsUnderUnlocking.contains(event.getBlock().getLocation())) {
            Rebus.Instance.sendLocalizedMsg(player, "Chest.LocationOccupied");
            return;
        }

        // Check if the player is already unlocking a chest.
        if (Rebus.chestManager().playersUnlocking.contains(player.getUniqueId())) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.AlreadyOpening");
            return;
        }

        // Check if the player has the required permission to place the chest.
        if (!player.hasPermission(chest.getPermission())) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }

        // Check if the player is on cooldown for opening this chest.
        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
        long remainingTime = Rebus.database().getCooldown(player.getUniqueId(), ECooldownType.OPEN, chest.getKey());
        if (remainingTime > 0 && !player.hasPermission("rebus.bypass.cooldown")) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
            return;
        }

        // Handle the placement of the chest.
        Rebus.chestManager().handlePlaceChest(player, chest, itemInHand, event.getBlock());
    }

    /**
     * Handles the BlockBreakEvent to prevent breaking blocks that are part of an unlocking chest.
     *
     * @param event The BlockBreakEvent triggered when a player breaks a block.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // If the event is already cancelled, do nothing.
        if (event.isCancelled()) {
            return;
        }

        // Cancel the event if the block is part of an unlocking chest.
        if (Rebus.chestManager().chestsUnderUnlocking.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}