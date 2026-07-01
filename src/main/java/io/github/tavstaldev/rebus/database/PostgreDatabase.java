package io.github.tavstaldev.rebus.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.database.models.ChestUsage;
import io.github.tavstaldev.rebus.database.models.Cooldown;
import io.github.tavstaldev.rebus.database.models.ECooldownType;
import io.github.tavstaldev.yggra.core.database.QueryCondition;
import io.github.tavstaldev.yggra.core.database.repositories.IRepository;
import io.github.tavstaldev.yggra.core.database.repositories.PostgreSqlRepository;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PostgreDatabase implements IRebusDatabase {
    private final Rebus _plugin;
    private final RebusConfig _config;
    private final YggraLogger _logger;
    private HikariDataSource _dataSource;

    private PostgreSqlRepository<UUID, Cooldown> _cooldowns;
    private PostgreSqlRepository<UUID, ChestUsage> _chestUsages;

    public PostgreDatabase(Rebus plugin) {

        _plugin = plugin;
        _config = plugin.config();
        _logger = _plugin.logger().withModule(MySqlDatabase.class);
    }

    /**
     * Loads the database configuration and initializes the connection pool.
     */
    @Override
    public void load() {
        String tablePrefix = _config.storageTablePrefix;
        _dataSource = createDataSource();
        _cooldowns = new PostgreSqlRepository<>(_plugin, Cooldown.class, _dataSource, 60, tablePrefix);
        _chestUsages = new PostgreSqlRepository<>(_plugin, ChestUsage.class, _dataSource, 60, tablePrefix);
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
    public HikariDataSource createDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", _config.storageHost, _config.storagePort, _config.storageDatabase));
            config.setUsername(_config.storageUsername);
            config.setPassword(_config.storagePassword);
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(_config.storagePool);
            config.setMaxLifetime(_config.storageMaxLifeTime);
            config.setConnectionTimeout(_config.storageConnectionTimeout);
            return new HikariDataSource(config);
        } catch (Exception ex) {
            _logger.error("Unknown error happened during the creation of database connection..", ex);
            return null;
        }
    }

    /**
     * Checks and creates the necessary database schema if it does not exist.
     */
    @Override
    public void checkSchema() {
        _cooldowns.checkSchema();
        _chestUsages.checkSchema();
    }

    @Override
    public @NonNull IRepository<UUID, Cooldown> cooldowns() {
        return _cooldowns;
    }

    @Override
    public @NotNull IRepository<UUID, ChestUsage> chestUsages() {
        return _chestUsages;
    }

    @Override
    public List<Cooldown> getCooldowns(UUID playerId) {
        return _cooldowns.findByCriteria(playerId + ":all", QueryCondition.eq("playerId", playerId));
    }

    @Override
    public List<ChestUsage> getChestUsages(UUID playerId) {
        return _chestUsages.findByCriteria(playerId + ":all", QueryCondition.eq("playerId", playerId));
    }

    @Override
    public long getCooldown(UUID playerId, String context, ECooldownType type, String chestKey) {
        List<Cooldown> cooldowns = getCooldowns(playerId);
        final var now = LocalDateTime.now();
        long result = 0;
        for (var cooldown : cooldowns) {
            if (cooldown.getType() != type)
                continue;

            if (!Objects.equals(cooldown.getContext(), context))
                continue;

            if (!Objects.equals(cooldown.getChest(), chestKey))
                continue;

            if (cooldown.getExpiresAt().isAfter(now)) {
                result = Duration.between(now, cooldown.getExpiresAt()).getSeconds();
                break;
            }
        }
        return result;
    }

    @Override
    public @Nullable ChestUsage getUsage(UUID playerId, String context, String chestKey) {
        List<ChestUsage> usages = getChestUsages(playerId);
        ChestUsage result = null;
        for (var usage : usages) {
            if (!Objects.equals(usage.getContext(), context))
                continue;

            if (!Objects.equals(usage.getChest(), chestKey))
                continue;

            result = usage;
            break;
        }
        return result;
    }
}
