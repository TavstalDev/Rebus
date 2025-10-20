package io.github.tavstaldev.rebus.models;

import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.rebus.gui.MainGUI;
import io.github.tavstaldev.rebus.gui.PreviewGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Represents a cache for a player, storing various states and cooldowns.
 */
public class PlayerCache {
    // The player associated with this cache.
    private final Player _player;

    // Indicates whether the GUI is currently opened for the player.
    private boolean _isGUIOpened;

    // The main menu GUI for the player.
    private SGMenu _mainMenu;

    // The preview chest associated with the player.
    private RebusChest _previewChest;

    // A set of items associated with the player.
    private Set<ItemStack> _items;

    // The preview menu GUI for the player.
    private SGMenu _previewMenu;

    // The current page of the preview menu.
    private int _previewPage;

    /**
     * Constructs a PlayerCache instance for the specified player.
     *
     * @param player The player associated with this cache.
     */
    public PlayerCache(Player player) {
        this._player = player;
        this._isGUIOpened = false;
        this._mainMenu = null;
        this._previewChest = null;
        this._previewMenu = null;
        this._previewPage = 1;
    }

    /**
     * Checks if the GUI is currently opened for the player.
     *
     * @return True if the GUI is opened, false otherwise.
     */
    public boolean isGuiOpened() {
        return _isGUIOpened;
    }

    /**
     * Sets the GUI opened state for the player.
     *
     * @param isGUIOpened True to mark the GUI as opened, false otherwise.
     */
    public void setGuiOpened(boolean isGUIOpened) {
        this._isGUIOpened = isGUIOpened;
    }

    /**
     * Retrieves the main menu GUI for the player, creating it if necessary.
     *
     * @return The main menu GUI.
     */
    public SGMenu getMainMenu() {
        if (_mainMenu == null) {
            _mainMenu = MainGUI.create(_player);
        }
        return _mainMenu;
    }

    /**
     * Retrieves the preview menu GUI for the player, creating it if necessary.
     *
     * @return The preview menu GUI.
     */
    public SGMenu getPreviewMenu() {
        if (_previewMenu == null) {
            _previewMenu = PreviewGUI.create(_player);
        }
        return _previewMenu;
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