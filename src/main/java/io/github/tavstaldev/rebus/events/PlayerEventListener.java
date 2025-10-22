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

/**
 * The PlayerEventListener class handles player-related events such as joining, quitting, and interacting with blocks.
 * It ensures proper management of player caches and interactions with custom chests.
 */
public class PlayerEventListener implements Listener {

    /**
     * Initializes the PlayerEventListener by registering it with the Bukkit plugin manager.
     */
    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerEventListener(), Rebus.Instance);
    }

    /**
     * Handles the PlayerJoinEvent to add a new player cache when a player joins the server.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the server.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerCache playerCache = new PlayerCache(player);
        PlayerCacheManager.add(player.getUniqueId(), playerCache);
    }

    /**
     * Handles the PlayerQuitEvent to remove the player cache when a player leaves the server.
     *
     * @param event The PlayerQuitEvent triggered when a player quits the server.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerCacheManager.remove(player.getUniqueId());
    }

    /**
     * Handles the PlayerInteractEvent to prevent interactions with blocks that are part of unlocking chests.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with a block.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        var clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        // Cancel the event if the clicked block is part of an unlocking chest.
        if (Rebus.chestManager().chestsUnderUnlocking.contains(clickedBlock.getLocation())) {
            event.setCancelled(true);
        }
    }
}