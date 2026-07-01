package io.github.tavstaldev.rebus.managers.economy;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Economy manager implementation using Vault.
 */
public class VaultManager implements IEconomyManager {
    private final Economy economy;

    /**
     * Creates a new Vault economy manager and attempts to find an economy provider.
     * Disables the plugin if no provider is found.
     *
     * @param plugin The plugin instance.
     */
    public VaultManager(Rebus plugin) {
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

    @Override
    public boolean enabled() {
        return economy != null;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        if (economy == null)
            return 0;
        return economy.getBalance(player);
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        if (economy == null)
            return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String getCurrencySingular() {
        if (economy == null)
            return null;
        return economy.currencyNameSingular();
    }

    @Override
    public String getCurrencyPlural() {
        if (economy == null)
            return null;
        return economy.currencyNamePlural();
    }
}
