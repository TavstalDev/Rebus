package io.github.tavstaldev.rebus.models;

import java.time.LocalDateTime;

/**
 * Represents a cooldown for a specific chest in a given context.
 */
public class Cooldown {
    // The context in which the cooldown is applied (e.g., storage context).
    private final String context;

    private final ECooldownType type;

    // The unique identifier of the chest associated with the cooldown.
    private final String chest;

    // The expiration time of the cooldown.
    private final LocalDateTime expiresAt;


    public Cooldown(String context, ECooldownType type, String chest, LocalDateTime expiresAt) {
        this.context = context;
        this.type = type;
        this.chest = chest;
        this.expiresAt = expiresAt;
    }

    /**
     * Retrieves the context of the cooldown.
     *
     * @return The context as a string.
     */
    public String getContext() {
        return context;
    }

    /**
     * Retrieves the chest associated with the cooldown.
     *
     * @return The chest identifier as a string.
     */
    public String getChest() {
        return chest;
    }

    public ECooldownType getType() {
        return type;
    }

    /**
     * Retrieves the expiration time of the cooldown.
     *
     * @return The expiration time as a LocalDateTime object.
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Checks if the cooldown has expired.
     *
     * @return True if the current time is after the expiration time, false otherwise.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
