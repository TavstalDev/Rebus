package io.github.tavstaldev.rebus.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.models.Cooldown;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MySqlDatabase implements IDatabase {
    private HikariDataSource _dataSource;
    private RebusConfig _config;
    private final PluginLogger _logger = Rebus.Logger().WithModule(MySqlDatabase.class);

    @Override
    public void load() {
        _config = Rebus.Config();
        _dataSource = CreateDataSource();
    }

    @Override
    public void unload() {
        if (_dataSource != null) {
            if (!_dataSource.isClosed())
                _dataSource.close();
        }
    }

    public HikariDataSource CreateDataSource() {
        try
        {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s", _config.storageHost,_config.storagePort, _config.storageDatabase));
            config.setUsername(_config.storageUsername);
            config.setPassword(_config.storagePassword);
            config.setMaximumPoolSize(10); // Pool size defaults to 10
            config.setMaxLifetime(30000);
            return new HikariDataSource(config);
        }
        catch (Exception ex)
        {
            _logger.Error(String.format("Unknown error happened during the creation of database connection...\n%s", ex.getMessage()));
            return null;
        }
    }

    @Override
    public void checkSchema() {
        try (Connection connection = _dataSource.getConnection())
        {
            // PlayerData
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
        }
        catch (Exception ex)
        {
            _logger.Error(String.format("Unknown error happened while creating tables...\n%s", ex.getMessage()));
        }
    }

    @Override
    public void addCooldown(UUID playerId, String chestKey, long seconds) {
        try (Connection connection =  _dataSource.getConnection())
        {
            String sql = String.format("INSERT INTO %s_cooldowns (PlayerId, Context, Chest, ExpiresAt) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE ExpiresAt = VALUES(ExpiresAt);",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set parameters for the prepared statement
                statement.setString(1, playerId.toString());
                statement.setString(2,  _config.storageContext);
                statement.setString(3,chestKey);
                statement.setTimestamp(4, Timestamp.valueOf((LocalDateTime.now().plusSeconds(seconds))));

                // Execute the query
                statement.executeUpdate();
            }
        }
        catch (Exception ex)
        {
            _logger.Error(String.format("Unknown error happened while adding tables...\n%s", ex.getMessage()));
        }
    }

    @Override
    public void removeCooldowns(UUID playerId, String chestKey) {
        try (Connection connection =  _dataSource.getConnection())
        {
            String sql = String.format("DELETE FROM %s_cooldowns WHERE PlayerId=? AND Context=? AND Chest=? LIMIT 1;",
                    _config.storageTablePrefix);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set parameters for the prepared statement
                statement.setString(1, playerId.toString());
                statement.setString(2, _config.storageContext);
                statement.setString(3,chestKey);

                // Execute the query
                statement.executeUpdate();
            }
        }
        catch (Exception ex)
        {
            _logger.Error(String.format("Unknown error happened while removing tables...\n%s", ex.getMessage()));
        }
    }

    @Override
    public Set<Cooldown> getCooldowns(UUID playerId) {
        Set<Cooldown> cooldowns = new HashSet<>();
        try (Connection connection =  _dataSource.getConnection())
        {
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
        }
        catch (Exception ex)
        {
            _logger.Error(String.format("Unknown error happened while finding cooldowns...\n%s", ex.getMessage()));
            return null;
        }
        return  cooldowns;
    }
}
