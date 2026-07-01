package io.github.tavstaldev.rebus.managers.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for economy manager implementations.
 */
public interface IEconomyManager {

    /**
     * Checks if the economy provider is available.
     *
     * @return True if the economy is enabled.
     */
    boolean enabled();

    /**
     * Gets the balance of a player.
     *
     * @param player The player to check.
     * @return The player's balance.
     */
    double getBalance(@NotNull Player player);

    /**
     * Checks if a player has at least the given amount.
     *
     * @param player The player to check.
     * @param amount The amount to check for.
     * @return True if the player has enough funds.
     */
    boolean has(@NotNull Player player, double amount);

    /**
     * Withdraws an amount from a player's balance.
     *
     * @param player The player to withdraw from.
     * @param amount The amount to withdraw.
     * @return True if the transaction was successful.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean withdraw(@NotNull Player player, double amount);

    /**
     * Deposits an amount into a player's balance.
     *
     * @param player The player to deposit to.
     * @param amount The amount to deposit.
     * @return True if the transaction was successful.
     */
    boolean deposit(@NotNull Player player, double amount);

    /**
     * Gets the singular name of the currency.
     *
     * @return The currency name, or null if not available.
     */
    String getCurrencySingular();

    /**
     * Gets the plural name of the currency.
     *
     * @return The currency name, or null if not available.
     */
    String getCurrencyPlural();
}
