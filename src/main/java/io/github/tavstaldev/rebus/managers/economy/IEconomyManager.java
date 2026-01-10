package io.github.tavstaldev.rebus.managers.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IEconomyManager {

    boolean enabled();

    double getBalance(@NotNull Player pLayer);

    boolean has(@NotNull Player player, double amount);

    boolean withdraw(@NotNull Player player, double amount);

    boolean deposit(@NotNull Player player, double amount);

    String getCurrencySingular();

    String getCurrencyPlural();
}
