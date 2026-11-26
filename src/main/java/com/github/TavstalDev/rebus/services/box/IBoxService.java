package com.github.TavstalDev.rebus.services.box;

import com.github.TavstalDev.rebus.models.Chest;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IBoxService {
    String getChestKey();

    @Nullable Chest getChest(String key);

    Map<String, Chest> getChests();

    Set<UUID> playersUnlocking();
    Set<Location> occupiedLocations();

    /**
     * Loads all chest configurations from the chests directory.
     */
    void load();

    /**
     * Gives a chest item to a player and updates their usage count in the database.
     *
     * @param player The player to give the chest to.
     * @param chest  The chest type to give.
     * @param amount The number of uses to grant.
     */
    void giveChest(Player player, Chest chest, int amount);

    /**
     * Handles the chest placement animation, sound effects, and the rewarding process.
     *
     * @param player The player who placed the chest.
     * @param chest  The chest type being placed.
     * @param item   The item stack used for placement.
     * @param block  The block where the chest was placed.
     */
    void handlePlaceChest(final Player player, final Chest chest, final ItemStack item, final Block block);
}
