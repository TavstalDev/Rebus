package io.github.tavstaldev.rebus.tasks;

import io.github.tavstaldev.rebus.managers.PlayerCacheManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CacheCleanTask extends BukkitRunnable {
    @Override
    public void run() {
        if (PlayerCacheManager.isMarkedForRemovalEmpty())
            return;

        for (var playerId : PlayerCacheManager.getMarkedForRemovalSet()) {
            var playerCache = PlayerCacheManager.get(playerId);
            if (playerCache == null)
            {
                PlayerCacheManager.unmarkForRemoval(playerId);
                continue;
            }

            PlayerCacheManager.remove(playerId);
            PlayerCacheManager.unmarkForRemoval(playerId);
        }
    }
}
