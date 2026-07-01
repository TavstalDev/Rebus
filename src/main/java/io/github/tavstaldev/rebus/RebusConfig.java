package io.github.tavstaldev.rebus;

import io.github.tavstaldev.yggra.core.configuration.YggraConfiguration;

public class RebusConfig extends YggraConfiguration {
    public RebusConfig(Rebus plugin) {
        super(plugin, "config.yml", null);
    }

    public String prefix;
    public boolean checkForUpdates, debug;

    public String storageType, storageContext, storageFilename, storageHost, storageDatabase, storageUsername, storagePassword, storageTablePrefix;
    public int storagePort, storageAutoClean, storagePool, storageMaxLifeTime, storageConnectionTimeout;

    public boolean storageRedisEnabled;
    public int storageRedisPort;
    public String storageRedisHost, storageRedisUsername, storageRedisPassword;

    public String npcName, npcSkin, npcSignature;

    @Override
    protected void loadDefaults() {
        // General
        resolve("general.locale", "eng");
        resolve("general.use-player-locale", true);
        checkForUpdates = resolveGet("general.check-updates", true);
        debug = resolveGet("general.debug-mode", false);
        prefix = resolveGet("general.prefix", "&bRebus &8»");

        // Storage - Database
        storageType = resolveGet("storage.db.type", "sqlite");
        storageContext = resolveGet("storage.db.context", "skypvp");
        storageHost = resolveGet("storage.db.host", "localhost");
        storagePort = resolveGet("storage.db.port", 3306);
        storageDatabase = resolveGet("storage.db.database", "minecraft");
        storageUsername = resolveGet("storage.db.username", "root");
        storagePassword = resolveGet("storage.db.password", "ascent");
        storageTablePrefix = resolveGet("storage.db.table-prefix", "rebus_");
        storageAutoClean = resolveGet("storage.db.auto-clean-interval", 3600);
        storagePool = resolveGet("storage.db.pool-settings.maximum-pool-size", 10);
        storageMaxLifeTime = resolveGet("storage.db.pool-settings.maximum-lifetime", 1800000);
        storageConnectionTimeout = resolveGet("storage.db.pool-settings.connection-timeout", 5000);

        // Storage - Redis
        storageRedisEnabled = resolveGet("storage.redis.enable", false);
        storageRedisHost = resolveGet("storage.redis.host", "localhost");
        storageRedisPort = resolveGet("storage.redis.port", 6379);
        storageRedisUsername = resolveGet("storage.redis.username", "root");
        storageRedisPassword = resolveGet("storage.redis.password", "ascent");


        // NPC
        npcName = resolveGet("npc.name", "&f_Rébusz_");
        npcSkin = resolveGet("npc.skin", "ewogICJ0aW1lc3RhbXAiIDogMTc0MDU4MzQxMzMyMiwKICAicHJvZmlsZUlkIiA6ICJiZDNhNWRmY2ZkZjg0NDczOTViZDJiZmUwNGY0YzAzMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJwcmVja3Jhc25vIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y4MzkzMGY3MWJmMjVkZDEzYjM2NGZmN2UwZTg3ZTg4Yjg3NTZjYmJiZjgyMjQxMGY0NzA0NWVjZjEyNzYzOTkiCiAgICB9CiAgfQp9");
        npcSignature = resolveGet("npc.signature", "s0iYOKakbeeLI3bIxokioac+R4yNiyYYkaubIfe5pBXJgUbFpxbVV40M/G7huGPMwbi6gHI6PgD4Q8n4BiGqWYcxwQ227n0MEnvQIerJgvB1gm/49BrgQ565ldixYWNQwHh0jkxErAVRqyd7Kb5vGup1Ba4QB7E2c4+kQnyDyBDHZ8RKc11EWIevdB5NZcrGLUON34mVy9D7wZihyUOOAvcAC8KrGX6wmvizZqcNtRTcymmNF1t7Zl+rxQxsBou3qMSze3RmDuhSKKy7oIjYc3pbag2uHAdhjWqEMI3qUkN2wADzr11vNYhqhs+uOKegHJMuWkMeeThEje20iMdpnz7Ut9VOZe+imPhHOX07Yi5mQblptBtYJkfhIOeRoh65Zv9w1aspks/KkSiRud0mpdr7ky8TmUWTy+YZgernKIWwuYk5YoxIXMcojmXsMUzWDV719ctbqncxfiaa3fvF2/IoI3AUn6w1Fc04sbcu54XiNOTR2WrdS8accwyX9zEzSqdvYKFl3bPshH6qKtf57d7V0VsGIwGfUU5wtfWjH6G1ylZFmi8ji1o1O/X5LV1R1bw1WsuAyN5GhyXWq8A+PhUEsG+/lH+qJ4WDvFSqmb/f7fEMAZG2kUW/ye823RSjchqXMy78i5pneS98DBGoO5MjG2T1yo9Rp6NWsVHHO0o=");

        // Chests
        // TODO: Add chest configuration
    }
}
