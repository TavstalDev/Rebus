package io.github.tavstaldev.rebus.database;

import io.github.tavstaldev.rebus.database.models.ChestUsage;
import io.github.tavstaldev.rebus.database.models.Cooldown;
import io.github.tavstaldev.rebus.database.models.ECooldownType;
import io.github.tavstaldev.yggra.core.database.IDatabase;
import io.github.tavstaldev.yggra.core.database.repositories.IRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IRebusDatabase extends IDatabase {

    @NotNull IRepository<UUID, Cooldown> cooldowns();

    @NotNull IRepository<UUID, ChestUsage> chestUsages();

    /**
     * Retrieves all cooldowns for a specific player from the database.
     *
     * @param playerId The UUID of the player.
     * @return A set of Cooldown objects representing the player's cooldowns.
     */
    List<Cooldown> getCooldowns(UUID playerId);

    List<ChestUsage> getChestUsages(UUID playerId);

    /**
     * Retrieves the remaining cooldown time for a specific player and chest.
     *
     * @param playerId The UUID of the player.
     * @param context The database context.
     * @param type The type of cooldown (e.g., ECooldownType).
     * @param chestKey The key identifying the chest associated with the cooldown.
     * @return The remaining cooldown time in seconds.
     */
    long getCooldown(UUID playerId, String context, ECooldownType type, String chestKey);

    @Nullable ChestUsage getUsage(UUID playerId, String context, String chestKey);
}
