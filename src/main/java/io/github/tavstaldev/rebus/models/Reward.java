package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.rebus.Rebus;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class Reward {
    private final int chance;
    private final Set<Integer> items;
    private Set<ItemStack> _stackCache;

    public Reward(int chance, Set<Integer> items) {
        this.chance = chance;
        this.items = items;
    }

    public int getChance() {
        return chance;
    }

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
