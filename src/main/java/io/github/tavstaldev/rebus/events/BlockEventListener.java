package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.models.ECooldownType;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.PermissionUtils;
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
 * Handles block-related events such as placing and breaking blocks.
 */
public class BlockEventListener implements Listener {

    /**
     * Initializes the event listener by registering it with the Bukkit plugin manager.
     */
    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new BlockEventListener(), Rebus.Instance);
    }

    /**
     * Handles the BlockPlaceEvent, validating and processing the placement of custom chests.
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
        if (!meta.getPersistentDataContainer().has(Rebus.ChestManager().getChestKey())) {
            return;
        }

        // Retrieve the chest key and corresponding chest object.
        String key = meta.getPersistentDataContainer().get(Rebus.ChestManager().getChestKey(), PersistentDataType.STRING);
        RebusChest chest = Rebus.ChestManager().getByKey(key);
        if (chest == null) {
            return;
        }

        // Cancel the event to prevent the block from being placed normally.
        event.setCancelled(true);

        // Check if the block location is already occupied by an unlocking chest.
        if (Rebus.ChestManager().chestsUnderUnlocking.contains(event.getBlock().getLocation())) {
            Rebus.Instance.sendLocalizedMsg(player, "Chest.LocationOccupied");
            return;
        }

        // Check if the player is already unlocking a chest.
        if (Rebus.ChestManager().playersUnlocking.contains(player.getUniqueId())) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.AlreadyOpening");
            return;
        }

        // Check if the player has the required permission to place the chest.
        if (!PermissionUtils.checkPermission(player, chest.getPermission())) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }

        // Check if the chest is on cooldown for the player.
        long remainingTime = Rebus.Database().getCooldown(player.getUniqueId(), ECooldownType.OPEN, chest.getKey());
        if (remainingTime > 0 && !PermissionUtils.checkPermission(player, "rebus.bypass.cooldown")) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
            return;
        }

        // Handle the placement of the chest.
        Rebus.ChestManager().handlePlaceChest(player, chest, itemInHand, event.getBlock());
    }

    /**
     * Handles the BlockBreakEvent, preventing the breaking of blocks that are under unlocking.
     *
     * @param event The BlockBreakEvent triggered when a player breaks a block.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the event is already cancelled.
        if (event.isCancelled()) {
            return;
        }

        // Cancel the event if the block is under unlocking.
        if (Rebus.ChestManager().chestsUnderUnlocking.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}