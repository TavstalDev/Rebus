package io.github.tavstaldev.rebus.managers.economy;

import io.github.tavstaldev.banyaszLib.api.BanyaszApi;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BanyaszManager implements IEconomyManager {
    private final PluginLogger logger = Rebus.Instance.getCustomLogger().withModule(this.getClass());
    private final BanyaszApi banyaszApi;

    public BanyaszManager() {
        // Check for BanyaszLib plugin
        logger.debug("Hooking into BanyaszLib...");
        if (!Bukkit.getPluginManager().isPluginEnabled("BanyaszLib")) {
            logger.error("BanyaszLib was not found! Unloading...");
            banyaszApi = null;
            Bukkit.getPluginManager().disablePlugin(Rebus.Instance);
            return;
        }

        banyaszApi = BanyaszApi.getInstance();
        logger.info("BanyaszLib found and hooked into it.");
    }

    @Override
    public boolean enabled() {
        return banyaszApi != null;
    }

    @Override
    public double getBalance(@NotNull Player player) {
        if (banyaszApi == null)
            return 0;
        return banyaszApi.getBalance(player.getUniqueId().toString());
    }

    @Override
    public boolean has(@NotNull Player player, double amount) {
        if (banyaszApi == null)
            return false;
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(@NotNull Player player, double amount) {
        if (banyaszApi == null)
            return false;
        return banyaszApi.decreaseBalance(player.getUniqueId().toString(), (int)amount);
    }

    @Override
    public boolean deposit(@NotNull Player player, double amount) {
        if (banyaszApi == null)
            return false;
        return banyaszApi.increaseBalance(player.getUniqueId().toString(), (int)amount);
    }

    @Override
    public String getCurrencySingular() {
        return "BÉ";
    }

    @Override
    public String getCurrencyPlural() {
        return "BÉ";
    }
}
