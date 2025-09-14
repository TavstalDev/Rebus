package io.github.tavstaldev.rebus.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Reward {
    private final int chance;
    private final List<ItemStack> items;

    public Reward(int chance, List<ItemStack> items) {
        this.chance = chance;
        this.items = items;
    }

    public int getChance() {
        return chance;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
