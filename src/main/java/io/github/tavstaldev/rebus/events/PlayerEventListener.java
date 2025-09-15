package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import io.github.tavstaldev.rebus.models.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEventListener implements Listener {
    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerEventListener(), Rebus.Instance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerCache playerCache = new PlayerCache(player);
        PlayerCacheManager.add(player.getUniqueId(), playerCache);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerCacheManager.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        var clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (Rebus.ChestManager().chestsUnderUnlocking.contains(clickedBlock.getLocation()))
        {
            event.setCancelled(true);
        }
    }
}
