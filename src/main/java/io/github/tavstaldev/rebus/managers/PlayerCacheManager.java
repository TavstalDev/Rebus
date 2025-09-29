package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.models.PlayerCache;

import java.util.*;

/**
 * Manages a cache of player data using a static map.
 * Provides methods to add, remove, clear, and retrieve player data.
 */
public class PlayerCacheManager {
    // A static map to store player data, keyed by the player's UUID.
    private static final Map<UUID, PlayerCache> _playerData = new HashMap<>();
    private static final Set<UUID> _markedForRemoval = new HashSet<>();

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

    /**
     * Marks a player for removal by adding their UUID to the removal set.
     *
     * @param playerId The UUID of the player to mark for removal.
     */
    public static void markForRemoval(UUID playerId) {
        _markedForRemoval.add(playerId);
    }

    /**
     * Unmarks a player for removal by removing their UUID from the removal set.
     *
     * @param playerId The UUID of the player to unmark for removal.
     */
    public static void unmarkForRemoval(UUID playerId) {
        _markedForRemoval.remove(playerId);
    }

    /**
     * Checks if a player is marked for removal.
     *
     * @param playerId The UUID of the player to check.
     * @return true if the player is marked for removal, false otherwise.
     */
    public static boolean isMarkedForRemoval(UUID playerId) {
        return _markedForRemoval.contains(playerId);
    }

    /**
     * Checks if the set of players marked for removal is empty.
     *
     * @return true if no players are marked for removal, false otherwise.
     */
    public static boolean isMarkedForRemovalEmpty() {
        return _markedForRemoval.isEmpty();
    }

    /**
     * Retrieves the set of UUIDs representing players marked for removal.
     *
     * @return A Set of UUIDs of players marked for removal.
     */
    public static Set<UUID> getMarkedForRemovalSet() {
        return new HashSet<>(_markedForRemoval); // Return a copy to prevent external modification
    }
}