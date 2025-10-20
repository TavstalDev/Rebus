package io.github.tavstaldev.rebus.tasks;

import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A task that periodically cleans up the player cache by removing marked entries.
 * This task is executed as a BukkitRunnable.
 */
public class CacheCleanTask extends BukkitRunnable {

    /**
     * The main logic of the task, executed when the task runs.
     * It checks if there are any players marked for removal in the cache.
     * If so, it iterates through the marked players, removes their cache entries,
     * and unmarks them for removal.
     */
    @Override
    public void run() {
        // If there are no players marked for removal, exit early.
        if (PlayerCacheManager.isMarkedForRemovalEmpty())
            return;

        // Iterate through the set of players marked for removal.
        for (var playerId : PlayerCacheManager.getMarkedForRemovalSet()) {
            // Retrieve the player's cache entry.
            var playerCache = PlayerCacheManager.get(playerId);

            // If the cache entry is null, unmark the player and continue.
            if (playerCache == null) {
                PlayerCacheManager.unmarkForRemoval(playerId);
                continue;
            }

            // Remove the player's cache entry and unmark them for removal.
            PlayerCacheManager.remove(playerId);
            PlayerCacheManager.unmarkForRemoval(playerId);
        }
    }
}