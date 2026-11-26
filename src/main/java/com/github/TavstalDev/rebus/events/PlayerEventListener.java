package com.github.TavstalDev.rebus.events;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

/**
 * Handles player-related events such as joining, quitting, and interacting with blocks.
 */
public class PlayerEventListener implements Listener {
    private final Rebus plugin;
    private final IBoxService boxService;

    public PlayerEventListener(Rebus plugin, IBoxService boxService) {
        this.plugin = plugin;
        this.boxService = boxService;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Cleans up the player's lock when they quit.
     *
     * @param event The quit event.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.removePlayerLock(event.getPlayer().getUniqueId());
    }

    /**
     * Handles the PlayerInteractEvent, canceling interactions with blocks that are under unlocking.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with a block.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        Set<Location> occupiedLocations = boxService.occupiedLocations();
        if (!occupiedLocations.contains(clickedBlock.getLocation()))
            return;
        event.setCancelled(true);
    }
}