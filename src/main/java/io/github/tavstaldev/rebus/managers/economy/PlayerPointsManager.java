package io.github.tavstaldev.rebus.managers.economy;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Economy manager implementation using PlayerPoints.
 */
public class PlayerPointsManager implements IEconomyManager {

    private final PlayerPointsAPI api;

    /**
     * Constructs a new PlayerPointsManager.
     *
     * @param plugin The Rebus plugin instance.
     */
    public PlayerPointsManager(Rebus plugin) {
        YggraLogger logger = plugin.logger().withModule(this.getClass());
        logger.debug("Setting up economy...");
        api = PlayerPoints.getInstance().getAPI();
        if (api == null) {
            logger.error("PlayerPoints API not found! Unloading...");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        logger.debug("Economy provider found.");
    }

    @Override
    public boolean enabled() {
        return api != null;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        return api.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        return api.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public String getCurrencySingular() {
        return "";
    }

    @Override
    public String getCurrencyPlural() {
        return "";
    }
}
