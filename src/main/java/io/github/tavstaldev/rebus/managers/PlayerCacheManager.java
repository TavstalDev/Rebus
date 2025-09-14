package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.models.PlayerCache;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages a cache of player data using a static map.
 * Provides methods to add, remove, clear, and retrieve player data.
 */
public class PlayerCacheManager {
    // A static map to store player data, keyed by the player's UUID.
    private static final Map<UUID, PlayerCache> _playerData = new HashMap<>();

    /**
     * Adds a player's data to the cache.
     *
     * @param playerId   The UUID of the player.
     * @param playerData The PlayerCache object containing the player's data.
     */
    public static void add(UUID playerId, PlayerCache playerData) {
        _playerData.put(playerId, playerData);
    }

    /**
     * Removes a player's data from the cache.
     *
     * @param playerId The UUID of the player to remove.
     */
    public static void remove(UUID playerId) {
        _playerData.remove(playerId);
    }

    /**
     * Clears all player data from the cache.
     */
    public static void clear() {
        _playerData.clear();
    }

    /**
     * Retrieves a player's data from the cache.
     *
     * @param playerId The UUID of the player to retrieve.
     * @return The PlayerCache object containing the player's data, or null if not found.
     */
    public static PlayerCache get(UUID playerId) {
        return _playerData.get(playerId);
    }
}