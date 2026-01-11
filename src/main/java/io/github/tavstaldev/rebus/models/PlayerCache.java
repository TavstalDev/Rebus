package io.github.tavstaldev.rebus.models;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Represents a cache for a player, storing various states and cooldowns.
 */
public class PlayerCache {
    // The player associated with this cache.
    private final Player _player;

    // The preview chest associated with the player.
    private RebusChest _previewChest;

    // A set of items associated with the player.
    private Set<ItemStack> _items;

    // The current page of the preview menu.
    private int _previewPage;

    /**
     * Constructs a PlayerCache instance for the specified player.
     *
     * @param player The player associated with this cache.
     */
    public PlayerCache(Player player) {
        this._player = player;
        this._previewChest = null;
        this._previewPage = 1;
    }

    /**
     * Retrieves the current page of the preview menu.
     *
     * @return The current preview page.
     */
    public int getPreviewPage() {
        return _previewPage;
    }

    /**
     * Sets the current page of the preview menu.
     *
     * @param page The page number to set.
     */
    public void setPreviewPage(int page) {
        this._previewPage = page;
    }

    /**
     * Retrieves the preview chest associated with the player.
     *
     * @return The preview chest.
     */
    public RebusChest getPreviewChest() {
        return _previewChest;
    }

    /**
     * Retrieves the set of items associated with the player.
     *
     * @return A set of ItemStack objects.
     */
    public Set<ItemStack> getItems() {
        return _items;
    }

    /**
     * Sets the preview chest associated with the player.
     *
     * @param chest The preview chest to set.
     */
    public void setPreviewChest(RebusChest chest) {
        this._previewChest = chest;
    }
}