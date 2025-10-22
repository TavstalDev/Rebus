package io.github.tavstaldev.rebus.tasks;

import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * CacheCleanTask is a scheduled task that handles the cleanup of player caches.
 * It extends BukkitRunnable to allow periodic execution within the Bukkit framework.
 */
public class CacheCleanTask extends BukkitRunnable {

    /**
     * The main logic of the task, executed when the task runs.
     * It checks for marked player caches and removes them if necessary.
     */
    @Override
    public void run() {

        // If there are no players marked for removal, exit the task.
        if (PlayerCacheManager.isMarkedForRemovalEmpty())
            return;

        // Iterate through the set of player IDs marked for removal.
        for (var playerId : PlayerCacheManager.getMarkedForRemovalSet()) {
            // Retrieve the player cache for the given player ID.
            var playerCache = PlayerCacheManager.get(playerId);

            // If the player cache does not exist, unmark the player ID and continue.
            if (playerCache == null) {
                PlayerCacheManager.unmarkForRemoval(playerId);
                continue;
            }

            // Remove the player cache and unmark the player ID.
            PlayerCacheManager.remove(playerId);
            PlayerCacheManager.unmarkForRemoval(playerId);
        }
    }
}