package com.github.TavstalDev.rebus.database;

import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.database.models.Cooldown;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.yggraLib.api.database.repositories.IRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IRebusDatabase extends com.github.TavstalDev.yggraLib.api.database.IDatabase {

    @NotNull IRepository<UUID, Cooldown> cooldowns();

    @NotNull IRepository<UUID, ChestUsage> chestUsages();

    List<Cooldown> getCooldowns(UUID playerId);

    List<ChestUsage> getChestUsages(UUID playerId);

    long getCooldown(UUID playerId, String context, ECooldownType type, String chestKey);

    @Nullable ChestUsage getUsage(UUID playerId, String context, String chestKey);
}
