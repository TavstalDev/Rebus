package com.github.TavstalDev.rebus.database;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.config.models.StorageConfig;
import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.database.models.Cooldown;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.yggraLib.api.database.QueryCondition;
import com.github.TavstalDev.yggraLib.api.database.repositories.IRepository;
import com.github.TavstalDev.yggraLib.api.database.repositories.impl.PostgreSqlRepository;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PostgreDatabase implements IRebusDatabase {
    private final Rebus plugin;
    private final RebusConfig config;
    private final YggraLogger logger;
    private HikariDataSource dataSource;

    private PostgreSqlRepository<UUID, Cooldown> cooldowns;
    private PostgreSqlRepository<UUID, ChestUsage> chestUsages;

    public PostgreDatabase(Rebus plugin) {

        this.plugin = plugin;
        config = plugin.config();
        logger = this.plugin.logger().withModule(MySqlDatabase.class);
    }

    /**
     * Loads the database configuration and initializes the connection pool.
     */
    @Override
    public void load() {
        String tablePrefix = config.storage.getTablePrefix();
        dataSource = createDataSource();
        cooldowns = new PostgreSqlRepository<>(plugin, Cooldown.class, dataSource, 60, tablePrefix);
        chestUsages = new PostgreSqlRepository<>(plugin, ChestUsage.class, dataSource, 60, tablePrefix);
    }

    /**
     * Unloads the database by closing the connection pool.
     */
    @Override
    public void unload() {
        if (dataSource != null) {
            if (!dataSource.isClosed())
                dataSource.close();
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
            StorageConfig sc = this.config.storage;
            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", sc.getHost(), sc.getPort(), sc.getDatabase()));
            config.setUsername(sc.getUsername());
            config.setPassword(sc.getPassword());
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(sc.getPool());
            config.setMaxLifetime(sc.getMaxLifeTime());
            config.setConnectionTimeout(sc.getConnectionTimeout());
            return new HikariDataSource(config);
        } catch (Exception ex) {
            logger.error("Unknown error happened during the creation of database connection..", ex);
            return null;
        }
    }

    /**
     * Checks and creates the necessary database schema if it does not exist.
     */
    @Override
    public void checkSchema() {
        cooldowns.checkSchema();
        chestUsages.checkSchema();
    }

    @Override
    public @NotNull IRepository<UUID, Cooldown> cooldowns() {
        return cooldowns;
    }

    @Override
    public @NotNull IRepository<UUID, ChestUsage> chestUsages() {
        return chestUsages;
    }

    @Override
    public List<Cooldown> getCooldowns(UUID playerId) {
        return cooldowns.findByCriteria(playerId + ":all", QueryCondition.eq("playerId", playerId));
    }

    @Override
    public List<ChestUsage> getChestUsages(UUID playerId) {
        return chestUsages.findByCriteria(playerId + ":all", QueryCondition.eq("playerId", playerId));
    }

    @Override
    public long getCooldown(UUID playerId, String context, ECooldownType type, String chestKey) {
        List<Cooldown> cooldowns = getCooldowns(playerId);
        final LocalDateTime now = LocalDateTime.now();
        long result = 0;
        for (Cooldown cooldown : cooldowns) {
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
        for (ChestUsage usage : usages) {
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
