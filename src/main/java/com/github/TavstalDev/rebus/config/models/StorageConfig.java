package com.github.TavstalDev.rebus.config.models;

import com.github.TavstalDev.yggraLib.bootstrap.YggraConfig;
import lombok.Getter;

/**
 * Typed representation of database and cache storage configuration settings.
 */
@Getter
public class StorageConfig {
    /** Database type identifier (e.g. sqlite, mysql, mariadb, postgres). */
    private final String type;
    /** Storage context identifier used by the plugin/runtime. */
    private final String context;
    /** Database host. */
    private final String host;
    /** Database name/schema. */
    private final String database;
    /** Database username. */
    private final String username;
    /** Database password. */
    private final String password;
    /** Prefix applied to database table names. */
    private final String tablePrefix;

    /** Database port. */
    private final int port;
    /** Automatic cleanup interval (seconds), as configured. */
    private final int autoClean;
    /** Maximum connection pool size. */
    private final int pool;
    /** Maximum connection lifetime in milliseconds. */
    private final int maxLifeTime;
    /** Connection timeout in milliseconds. */
    private final int connectionTimeout;

    /** Redis cache configuration. */
    private final RedisConfig redis;

    /**
     * Loads storage settings from the provided configuration.
     *
     * @param parent the parent configuration source
     */
    public StorageConfig(YggraConfig parent) {
        type = parent.resolveGet("storage.db.type", "sqlite");
        context = parent.resolveGet("storage.db.context", "survival");
        host = parent.resolveGet("storage.db.host", "localhost");
        port = parent.resolveGet("storage.db.port", 3306);
        database = parent.resolveGet("storage.db.database", "minecraft");
        username = parent.resolveGet("storage.db.username", "root");
        password = parent.resolveGet("storage.db.password", "ascent");
        tablePrefix = parent.resolveGet("storage.db.table-prefix", "rebus_");
        autoClean = parent.resolveGet("storage.db.auto-clean-interval", 3600);
        pool = parent.resolveGet("storage.db.pool-settings.maximum-pool-size", 10);
        maxLifeTime = parent.resolveGet("storage.db.pool-settings.maximum-lifetime", 1800000);
        connectionTimeout = parent.resolveGet("storage.db.pool-settings.connection-timeout", 5000);
        redis = new RedisConfig(parent);
    }

    /**
     * Creates a storage configuration model with explicit values.
     *
     * @param type              database type identifier
     * @param context           storage context identifier
     * @param host              database host
     * @param database          database name/schema
     * @param username          database username
     * @param password          database password
     * @param tablePrefix       table name prefix
     * @param port              database port
     * @param autoClean         automatic cleanup interval
     * @param pool              maximum pool size
     * @param maxLifeTime       maximum connection lifetime in milliseconds
     * @param connectionTimeout connection timeout in milliseconds
     * @param redis             Redis configuration
     */
    public StorageConfig(String type, String context, String host, String database, String username, String password, String tablePrefix, int port, int autoClean, int pool, int maxLifeTime, int connectionTimeout, RedisConfig redis) {
        this.type = type;
        this.context = context;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
        this.port = port;
        this.autoClean = autoClean;
        this.pool = pool;
        this.maxLifeTime = maxLifeTime;
        this.connectionTimeout = connectionTimeout;
        this.redis = redis;
    }
}
