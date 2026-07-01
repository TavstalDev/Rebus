package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a prize entry with a key, weight, and amount range.
 */
@SuppressWarnings("ClassCanBeRecord")
@Getter
public class Prize {
    private final String key;

    private final int weight;

    private final int minAmount;

    private final int maxAmount;

    /**
     * Creates a prize with the specified key, weight, and amount range.
     *
     * @param key The unique identifier for the prize.
     * @param weight The weight used for randomized selection.
     * @param minAmount The minimum amount to grant.
     * @param maxAmount The maximum amount to grant.
     */
    public Prize(String key, int weight, int minAmount, int maxAmount) {
        this.key = key;
        this.weight = weight;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    /**
     * Creates a prize with a default amount range of 1.
     *
     * @param key The unique identifier for the prize.
     * @param weight The weight used for randomized selection.
     */
    public Prize(String key, int weight) {
        this(key, weight, 1, 1);
    }

    /**
     * Creates a prize from a configuration map.
     *
     * @param map The configuration map containing prize data.
     * @param logger The logger for error reporting.
     * @return The parsed prize, or null if parsing failed.
     */
    public static @Nullable Prize fromMap(Map<String, Object> map, YggraLogger logger) {
        try {
            String key = (String) map.get("key");
            int weight = (int) map.getOrDefault("weight", 0);

            if (map.containsKey("amount")) {
                int amount = (int) map.getOrDefault("amount", 1);
                return new Prize(key, weight, amount, amount);
            } else {
                int minAmount = (int) map.getOrDefault("minAmount", 1);
                int maxAmount = (int) map.getOrDefault("maxAmount", 1);
                return new Prize(key, weight, minAmount, maxAmount);
            }
        }
        catch (Exception ex) {
            logger.error("Failed to load prize from map: " + map, ex);
            return  null;
        }
    }
}
