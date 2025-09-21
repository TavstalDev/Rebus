package io.github.tavstaldev.rebus.database;

import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.models.Cooldown;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the SQLite database implementation for managing cooldowns and other data.
 */
public class SqlLiteDatabase implements IDatabase {
    private RebusConfig _config;
    private final PluginLogger _logger = Rebus.Logger().WithModule(SqlLiteDatabase.class);

    /**
     * Loads the database configuration.
     */
    @Override
    public void load() {
        _config = Rebus.Config();
    }

    /**
     * Unloads the database. Currently, no specific actions are performed.
     */
    @Override
    public void unload() {}

    /**
     * Creates a connection to the SQLite database.
     *
     * @return A Connection object to the database, or null if an error occurs.
     */
    public Connection CreateConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(String.format("jdbc:sqlite:plugins/Rebus/%s.db", _config.storageFilename));
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while creating db connection...\n%s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Checks and creates the necessary database schema if it does not exist.
     */
    @Override
    public void checkSchema() {
        try (Connection connection = CreateConnection()) {
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s_cooldowns (" +
                            "PlayerId VARCHAR(36), " +
                            "Context VARCHAR(32), " +
                            "Chest VARCHAR(32), " +
                            "ExpiresAt DATETIME," +
                            "PRIMARY KEY (PlayerId, Context, Chest));",
                    _config.storageTablePrefix
            );
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while creating tables...\n%s", ex.getMessage()));
        }
    }

    /**
     * Adds a cooldown entry for a player in the database.
     *
     * @param playerId The UUID of the player.
     * @param chestKey The key of the chest associated with the cooldown.
     * @param seconds  The duration of the cooldown in seconds.
     */
    @Override
    public void addCooldown(UUID playerId, String chestKey, long seconds) {
        try (Connection connection = CreateConnection()) {
            String sql = String.format("INSERT OR REPLACE INTO %s_cooldowns (PlayerId, Context, Chest, ExpiresAt) " +
                            "VALUES (?, ?, ?, ?);",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.setString(3, chestKey);
                statement.setTimestamp(4, Timestamp.valueOf((LocalDateTime.now().plusSeconds(seconds))));
                statement.executeUpdate();
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while adding tables...\n%s", ex.getMessage()));
        }
    }

    /**
     * Removes cooldown entries for a specific player and chest from the database.
     *
     * @param playerId The UUID of the player.
     * @param chestKey The key of the chest associated with the cooldown.
     */
    @Override
    public void removeCooldowns(UUID playerId, String chestKey) {
        try (Connection connection = CreateConnection()) {
            String sql = String.format("DELETE FROM %s_cooldowns WHERE PlayerId=? AND Context=? AND Chest=? LIMIT 1;",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.setString(3, chestKey);
                statement.executeUpdate();
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while removing tables...\n%s", ex.getMessage()));
        }
    }

    /**
     * Retrieves all cooldowns for a specific player from the database.
     *
     * @param playerId The UUID of the player.
     * @return A set of Cooldown objects representing the player's cooldowns.
     */
    @Override
    public Set<Cooldown> getCooldowns(UUID playerId) {
        Set<Cooldown> cooldowns = new HashSet<>();
        try (Connection connection = CreateConnection()) {
            String sql = String.format("SELECT * FROM %s_cooldowns WHERE PlayerId=? AND Context=?;",
                    _config.storageTablePrefix);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        cooldowns.add(new Cooldown(result.getString("Context"), result.getString("Chest"), result.getTimestamp("ExpiresAt").toLocalDateTime()));
                    }
                }
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while finding cooldowns...\n%s", ex.getMessage()));
            return null;
        }
        return cooldowns;
    }
}