package io.github.tavstaldev.rebus.database;

import io.github.tavstaldev.rebus.models.Cooldown;

import java.util.Set;
import java.util.UUID;

public interface IDatabase {
    void load();

    void unload();

    void checkSchema();

    void addCooldown(UUID playerId, String chestKey, long seconds);

    void removeCooldowns(UUID playerId, String chestKey);

    Set<Cooldown> getCooldowns(UUID playerId);
}
