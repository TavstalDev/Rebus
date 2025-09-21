package io.github.tavstaldev.rebus.database;

import io.github.tavstaldev.rebus.models.Cooldown;

import java.util.Set;
import java.util.UUID;

/**
 * Interface representing the database operations for managing cooldowns and other data.
 */
public interface IDatabase {

    /**
     * Loads the database configuration and initializes any necessary resources.
     */
    void load();

    /**
     * Unloads the database and releases any allocated resources.
     */
    void unload();

    /**
     * Checks and creates the necessary database schema if it does not exist.
     */
    void checkSchema();

    /**
     * Adds a cooldown entry for a player in the database.
     *
     * @param playerId The UUID of the player.
     * @param chestKey The key of the chest associated with the cooldown.
     * @param seconds  The duration of the cooldown in seconds.
     */
    void addCooldown(UUID playerId, String chestKey, long seconds);

    /**
     * Removes cooldown entries for a specific player and chest from the database.
     *
     * @param playerId The UUID of the player.
     * @param chestKey The key of the chest associated with the cooldown.
     */
    void removeCooldowns(UUID playerId, String chestKey);

    /**
     * Retrieves all cooldowns for a specific player from the database.
     *
     * @param playerId The UUID of the player.
     * @return A set of Cooldown objects representing the player's cooldowns.
     */
    Set<Cooldown> getCooldowns(UUID playerId);
}