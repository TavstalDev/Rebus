package com.github.TavstalDev.rebus.services.economy;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerPointsService implements IEconomyService {
    private final PlayerPointsAPI api;

    public PlayerPointsService(Rebus plugin) {
        YggraLogger logger = plugin.logger().withModule(this.getClass());
        logger.debug("Setting up economy...");
        api = PlayerPoints.getInstance().getAPI();
        if (api == null) {
            logger.error("PlayerPointsAPI not found! Unloading...");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        logger.debug("Economy provider found.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean enabled() {
        return api != null;
    }

    /** {@inheritDoc} */
    @Override
    public double getBalance(@NotNull Player player) {
        return api.look(player.getUniqueId());
    }

    /** {@inheritDoc} */
    @Override
    public boolean has(@NotNull Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /** {@inheritDoc} */
    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        return api.take(player.getUniqueId(), (int) amount);
    }

    /** {@inheritDoc} */
    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        return api.give(player.getUniqueId(), (int) amount);
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrencySingular() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrencyPlural() {
        return "";
    }
}
