package io.github.tavstaldev.rebus.models;

/**
 * Enum representing different types of cooldowns in the application.
 * <ul>
 *     <li>BUY - Represents a cooldown for purchasing actions.</li>
 *     <li>OPEN - Represents a cooldown for opening actions.</li>
 * </ul>
 */
public enum ECooldownType {
    /**
     * Cooldown type for purchase-related actions.
     */
    BUY,

    /**
     * Cooldown type for opening-related actions.
     */
    OPEN
}