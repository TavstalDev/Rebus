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
 * Handles player-related events such as joining, quitting, and interacting with blocks.
 */
public class PlayerEventListener implements Listener {

    /**
     * Initializes the event listener by registering it with the Bukkit plugin manager.
     */
    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerEventListener(), Rebus.Instance);
    }

    /**
     * Handles the PlayerJoinEvent, creating and adding a PlayerCache for the joining player.
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
     * Handles the PlayerQuitEvent, removing the PlayerCache for the quitting player.
     *
     * @param event The PlayerQuitEvent triggered when a player leaves the server.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerCacheManager.remove(player.getUniqueId());
    }

    /**
     * Handles the PlayerInteractEvent, canceling interactions with blocks that are under unlocking.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with a block.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        var clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (Rebus.ChestManager().chestsUnderUnlocking.contains(clickedBlock.getLocation())) {
            event.setCancelled(true);
        }
    }
}