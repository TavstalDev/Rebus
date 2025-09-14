package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.PlayerCache;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.TimeUtil;
import io.github.tavstaldev.rebus.util.PermissionUtils;
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

public class BlockEventListener implements Listener {

    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new BlockEventListener(), Rebus.Instance);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null || itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            return;
        }

        var meta = itemInHand.getItemMeta();
        if (!meta.getPersistentDataContainer().has(Rebus.Chests().getChestKey())) {
            return;
        }
        String key = meta.getPersistentDataContainer().get(Rebus.Chests().getChestKey(), PersistentDataType.STRING);

        RebusChest chest = Rebus.Chests().getByKey(key);
        if (chest == null) {
            return;
        }

        if (Rebus.Chests().chestsUnderUnlocking.contains(event.getBlock().getLocation()))
        {
            Rebus.Instance.sendLocalizedMsg(player, "Chest.LocationOccupied");
            return;
        }

        // TODO: Add player chest placing duplicate check
        // Chests.AlreadyOpening

        event.setCancelled(true);
        if (!PermissionUtils.checkPermission(player, chest.getPermission())) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }

        PlayerCache cache = PlayerCacheManager.get(player.getUniqueId());
        long remainingTime = cache.getCooldown(chest);
        if (remainingTime > 0) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.Cooldown", Map.of("time", TimeUtil.formatDuration(player, remainingTime)));
            return;
        }

        Rebus.Chests().handlePlaceChest(player, chest, itemInHand, event.getBlock());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        if (Rebus.Chests().chestsUnderUnlocking.contains(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }
}
