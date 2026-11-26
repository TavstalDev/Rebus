package com.github.TavstalDev.rebus.services.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IEconomyService {

    boolean enabled();

    double getBalance(@NotNull Player player);

    boolean has(@NotNull Player player, double amount);

    boolean withdraw(@NotNull Player player, double amount);

    boolean deposit(@NotNull Player player, double amount);

    String getCurrencySingular();

    String getCurrencyPlural();
}
