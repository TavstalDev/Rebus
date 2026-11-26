package com.github.TavstalDev.rebus.services.economy;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class VaultEconomyService implements IEconomyService {
    private final Economy economy;

    public VaultEconomyService (Rebus plugin) {
        YggraLogger logger = plugin.logger().withModule(this.getClass());
        logger.debug("Setting up economy...");
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        logger.debug("Economy provider: " + economyProvider);
        if (economyProvider == null) {
            logger.error("No economy provider found! Unloading...");
            economy = null;
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        logger.debug("Economy provider found.");
        economy = economyProvider.getProvider();
    }

    /** {@inheritDoc} */
    @Override
    public boolean enabled() {
        return economy != null;
    }

    /** {@inheritDoc} */
    @Override
    public double getBalance(@NotNull Player player) {
        if (economy == null)
            return 0;
        return economy.getBalance(player);
    }

    /** {@inheritDoc} */
    @Override
    public boolean has(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.has(player, amount);
    }

    /** {@inheritDoc} */
    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** {@inheritDoc} */
    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrencySingular() {
        if (economy == null)
            return null;
        return economy.currencyNameSingular();
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrencyPlural() {
        if (economy == null)
            return null;
        return economy.currencyNamePlural();
    }
}
