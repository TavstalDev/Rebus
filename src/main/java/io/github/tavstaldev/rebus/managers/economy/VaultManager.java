package io.github.tavstaldev.rebus.managers.economy;

import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class VaultManager implements IEconomyManager {
    private final PluginLogger logger = Rebus.Instance.getCustomLogger().withModule(this.getClass());
    private final Economy economy;

    public VaultManager() {
        logger.debug("Setting up economy...");
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        logger.debug("Economy provider: " + economyProvider);
        if (economyProvider == null) {
            logger.error("No economy provider found! Unloading...");
            economy = null;
            Bukkit.getPluginManager().disablePlugin(Rebus.Instance);
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
