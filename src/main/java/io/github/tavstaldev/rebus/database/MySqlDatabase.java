package io.github.tavstaldev.rebus.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.models.Cooldown;
import io.github.tavstaldev.rebus.models.ECooldownType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Represents the MySQL database implementation for managing cooldowns and other data.
 * Utilizes HikariCP for efficient database connection pooling.
 */
public class MySqlDatabase implements IDatabase {
    private HikariDataSource _dataSource;
    private final Cache<@NotNull UUID, Set<Cooldown>> _playerCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    private RebusConfig _config;
    private final PluginLogger _logger = Rebus.Logger().WithModule(MySqlDatabase.class);

    /**
     * Loads the database configuration and initializes the connection pool.
     */
    @Override
    public void load() {
        _config = Rebus.Config();
        _dataSource = CreateDataSource();
    }

    /**
     * Unloads the database by closing the connection pool.
     */
    @Override
    public void unload() {
        if (_dataSource != null) {
            if (!_dataSource.isClosed())
                _dataSource.close();
        }
    }

    /**
     * Creates a HikariCP data source for managing database connections.
     *
     * @return A HikariDataSource object, or null if an error occurs.
     */
    public HikariDataSource CreateDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s", _config.storageHost, _config.storagePort, _config.storageDatabase));
            config.setUsername(_config.storageUsername);
            config.setPassword(_config.storagePassword);
            config.setMaximumPoolSize(10); // Pool size defaults to 10
            config.setMaxLifetime(30000);
            return new HikariDataSource(config);
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened during the creation of database connection...\n%s", ex.getMessage()));
            return null;
        }
    }

    /**
     * Checks and creates the necessary database schema if it does not exist.
     */
    @Override
    public void checkSchema() {
        try (Connection connection = _dataSource.getConnection()) {
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s_cooldowns (" +
                            "PlayerId VARCHAR(36), " +
                            "Context VARCHAR(32), " +
                            "Type VARCHAR(16), " +
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
     * Adds a cooldown for a specific player in the database.
     *
     * @param playerId The UUID of the player.
     * @param type The type of cooldown (e.g., ECooldownType).
     * @param chestKey The key identifying the chest associated with the cooldown.
     * @param seconds The duration of the cooldown in seconds.
     */
    @Override
    public void addCooldown(UUID playerId, ECooldownType type, String chestKey, long seconds) {
        try (Connection connection = _dataSource.getConnection()) {
            String sql = String.format("INSERT INTO %s_cooldowns (PlayerId, Context, Type, Chest, ExpiresAt) " +
                            "VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE ExpiresAt = VALUES(ExpiresAt), Type = VALUES(Type);",
                    _config.storageTablePrefix);

            var cooldownExpiresAt = LocalDateTime.now().plusSeconds(seconds);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.setString(3, type.name());
                statement.setString(4, chestKey);
                statement.setTimestamp(5, Timestamp.valueOf(cooldownExpiresAt));
                statement.executeUpdate();
            }

            var cache = _playerCache.getIfPresent(playerId);
            if (cache == null) {
                _playerCache.put(playerId, Set.of(new Cooldown(_config.storageContext, type, chestKey, cooldownExpiresAt)));
            }
            else {
                cache.add(new Cooldown(_config.storageContext, type, chestKey, cooldownExpiresAt));
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while adding tables...\n%s", ex.getMessage()));
        }
    }

    /**
     * Removes a specific cooldown for a player from the database.
     *
     * @param playerId The UUID of the player.
     * @param type The type of cooldown to remove (e.g., ECooldownType).
     * @param chestKey The key identifying the chest associated with the cooldown.
     */
    @Override
    public void removeCooldowns(UUID playerId, ECooldownType type, String chestKey) {
        try (Connection connection = _dataSource.getConnection()) {
            String sql = String.format("DELETE FROM %s_cooldowns WHERE PlayerId=? AND Context=? AND Type=? AND Chest=? LIMIT 1;",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.setString(3, type.name());
                statement.setString(4, chestKey);
                statement.executeUpdate();
            }

            var cache = _playerCache.getIfPresent(playerId);
            if (cache != null) {
                cache.removeIf(x -> x.getType() == type && x.getChest().equals(chestKey) && x.getContext().equals(_config.storageContext));
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while removing tables...\n%s", ex.getMessage()));
        }
    }

    /**
     * Removes all cooldowns for a specific player from the database and invalidates the cache.
     *
     * @param playerId The UUID of the player whose cooldowns are to be removed.
     */
    @Override
    public void removeAllCooldowns(UUID playerId) {
        try (Connection connection = _dataSource.getConnection()) {
            String sql = String.format("DELETE FROM %s_cooldowns WHERE PlayerId=? AND Context=?;",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.executeUpdate();
            }

            _playerCache.invalidate(playerId);
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
        var data = _playerCache.getIfPresent(playerId);
        if (data != null && !data.isEmpty()) {
            return data;
        }

        Set<Cooldown> cooldowns = new HashSet<>();
        try (Connection connection = _dataSource.getConnection()) {
            String sql = String.format("SELECT * FROM %s_cooldowns WHERE PlayerId=? AND Context=?;",
                    _config.storageTablePrefix);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        cooldowns.add(new Cooldown(result.getString("Context"), ECooldownType.valueOf(result.getString("Type")), result.getString("Chest"), result.getTimestamp("ExpiresAt").toLocalDateTime()));
                    }
                }
            }
        } catch (Exception ex) {
            _logger.Error(String.format("Unknown error happened while finding cooldowns...\n%s", ex.getMessage()));
            return null;
        }

        _playerCache.put(playerId, cooldowns);
        return cooldowns;
    }

    /**
     * Retrieves the remaining cooldown time for a specific player and chest.
     * If the cooldown has expired, it is removed from the database.
     *
     * @param playerId The UUID of the player.
     * @param type The type of cooldown (e.g., ECooldownType).
     * @param chestKey The key identifying the chest associated with the cooldown.
     * @return The remaining cooldown time in seconds. Returns 0 if no cooldown exists or it has expired.
     */
    @Override
    public long getCooldown(UUID playerId, ECooldownType type, String chestKey) {
        long cooldown = 0;
        final Set<Cooldown> cooldowns = getCooldowns(playerId);
        if (cooldowns == null || cooldowns.isEmpty())
            return cooldown;
        final var now = LocalDateTime.now();
        for (Cooldown cd : cooldowns) {
            if (cd.getType() == type && cd.getChest().equals(chestKey) && cd.getContext().equals(_config.storageContext)) {
                if (cd.getExpiresAt().isAfter(now)) {
                    cooldown = Duration.between(now, cd.getExpiresAt()).abs().getSeconds();
                }
                else {
                    removeCooldowns(playerId, type, chestKey);
                }
                break;
            }
        }
        return cooldown;
    }
}