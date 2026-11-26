package com.github.TavstalDev.rebus.config.models;

import com.github.TavstalDev.yggraLib.bootstrap.YggraConfig;
import lombok.Getter;

/**
 * Typed representation of Redis cache configuration settings.
 */
@Getter
public class RedisConfig {
    /** Whether Redis caching is enabled. */
    private final boolean enabled;
    /** Redis server port. */
    private final int port;
    /** Redis server host. */
    private final String host;
    /** Redis username (if required by the server/setup). */
    private final String username;
    /** Redis password (if required by the server/setup). */
    private final String password;

    /**
     * Loads Redis settings from the provided configuration.
     *
     * @param parent the parent configuration source
     */
    public RedisConfig(YggraConfig parent) {
        enabled = parent.resolveGet("storage.redis.enable", false);
        host = parent.resolveGet("storage.redis.host", "localhost");
        port = parent.resolveGet("storage.redis.port", 6379);
        username = parent.resolveGet("storage.redis.username", "root");
        password = parent.resolveGet("storage.redis.password", "ascent");
    }

    /**
     * Creates a Redis configuration model with explicit values.
     *
     * @param enabled  whether Redis is enabled
     * @param port     Redis server port
     * @param host     Redis server host
     * @param username Redis username
     * @param password Redis password
     */
    public RedisConfig(boolean enabled, int port, String host, String username, String password) {
        this.enabled = enabled;
        this.port = port;
        this.host = host;
        this.username = username;
        this.password = password;
    }
}
