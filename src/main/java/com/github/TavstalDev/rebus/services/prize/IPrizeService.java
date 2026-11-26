package com.github.TavstalDev.rebus.services.prize;

import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.models.PrizeData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface IPrizeService {

    /**
     * Loads prizes from the prizes.yml file.
     *
     * @return True if the prizes were loaded successfully, false otherwise.
     */
    boolean load();

    /**
     * Gets a prize by its key.
     *
     * @param key The prize key.
     * @return The prize data, or null if not found.
     */
    @Nullable PrizeData get(String key);

    /**
     * Rewards a player with a randomly selected prize from the given chest.
     * Handles item giving, command execution, cooldowns, and usage tracking.
     *
     * @param player The player to reward.
     * @param chest  The chest from which the prize is selected.
     * @return True if the reward was given successfully, false otherwise.
     */
    boolean reward(Player player, Chest chest);
}
