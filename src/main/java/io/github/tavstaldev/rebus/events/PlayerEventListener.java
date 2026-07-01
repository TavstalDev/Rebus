package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.ChestManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player-related events such as joining, quitting, and interacting with blocks.
 */
public class PlayerEventListener implements Listener {
    private final Rebus plugin;
    private final ChestManager chestManager;

    /**
     * Creates the player event listener and registers it with the server.
     *
     * @param plugin       The plugin instance.
     * @param chestManager The chest manager for checking unlocking states.
     */
    public PlayerEventListener(Rebus plugin, ChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
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
        var clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        if (chestManager.chestsUnderUnlocking.contains(clickedBlock.getLocation()))
            event.setCancelled(true);
    }
}