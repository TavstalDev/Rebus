package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.rebus.Rebus;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Represents a reward with a chance of being obtained and a set of item IDs.
 */
public class Reward {
    // The chance of obtaining this reward.
    private final int chance;

    // The set of item IDs associated with this reward.
    private final Set<Integer> items;

    // Cached set of ItemStacks corresponding to the item IDs.
    private Set<ItemStack> _stackCache;

    /**
     * Constructs a Reward instance.
     *
     * @param chance The chance of obtaining this reward.
     * @param items  The set of item IDs associated with this reward.
     */
    public Reward(int chance, Set<Integer> items) {
        this.chance = chance;
        this.items = items;
    }

    /**
     * Gets the chance of obtaining this reward.
     *
     * @return The chance as an integer.
     */
    public int getChance() {
        return chance;
    }

    /**
     * Gets the set of ItemStacks corresponding to the item IDs.
     * If the cache is available, it returns the cached ItemStacks.
     * Otherwise, it retrieves the ItemStacks from the ChestManager and caches them.
     *
     * @return A set of ItemStacks.
     */
    public Set<ItemStack> getItemStacks() {
        if (_stackCache != null && !_stackCache.isEmpty())
            return _stackCache;

        Set<ItemStack> itemStacks = new java.util.HashSet<>();
        var itemsTable = Rebus.ChestManager().getItemTable();
        for (Integer itemId : items) {
            ItemStack itemStack = itemsTable.get(itemId);
            if (itemStack != null) {
                itemStacks.add(itemStack);
            }
        }
        _stackCache = itemStacks;
        return itemStacks;
    }
}