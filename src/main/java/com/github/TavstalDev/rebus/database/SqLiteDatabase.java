package com.github.TavstalDev.rebus.database;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.database.models.Cooldown;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.yggraLib.api.database.QueryCondition;
import com.github.TavstalDev.yggraLib.api.database.repositories.IRepository;
import com.github.TavstalDev.yggraLib.api.database.repositories.impl.SqLiteRepository;
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

/**
 * Represents the SQLite database implementation for managing cooldowns and other data.
 */
public class SqLiteDatabase implements IRebusDatabase {
    private final Rebus plugin;
    private final YggraLogger logger;
    private final RebusConfig config;
    private SqLiteRepository<UUID, Cooldown> cooldowns;
    private SqLiteRepository<UUID, ChestUsage> chestUsages;

    public SqLiteDatabase(Rebus plugin) throws ClassNotFoundException {
        this.plugin = plugin;
        this.logger = this.plugin.logger().withModule(SqLiteDatabase.class);
        this.config = plugin.config();
        Class.forName("org.sqlite.JDBC");
    }

    /**
     * Loads the database configuration.
     */
    @Override
    public void load() {
        String tablePrefix = config.storage.getTablePrefix();
        HikariDataSource _dataSource = createDataSource();
        cooldowns = new SqLiteRepository<>(plugin, Cooldown.class, _dataSource, 60, tablePrefix);
        chestUsages = new SqLiteRepository<>(plugin, ChestUsage.class, _dataSource, 60, tablePrefix);
    }
    /**
     * Unloads the database. Currently, no specific actions are performed.
     */
    @Override
    public void unload() {}

    /**
     * Creates a HikariCP data source for managing database connections.
     *
     * @return A HikariDataSource object, or null if an error occurs.
     */
    public HikariDataSource createDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:sqlite:plugins/%s/%s.db", plugin.name(), plugin.name()));
            config.setMaximumPoolSize(4);

            // Disable features and validation checks unsupported by older SQLite JDBC drivers
            config.setConnectionTestQuery("SELECT 1");
            config.setInitializationFailTimeout(-1);
            config.validate();

            config.setConnectionInitSql(
                    "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000;"
            );

            return new HikariDataSource(config);
        } catch (Exception ex) {
            logger.error("Unknown error happened during the creation of database connection..", ex);
            return null;
        }
    }

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