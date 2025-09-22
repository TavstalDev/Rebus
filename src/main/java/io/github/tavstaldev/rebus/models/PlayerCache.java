package io.github.tavstaldev.rebus.models;

import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.rebus.gui.MainGUI;
import org.bukkit.entity.Player;

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

    /**
     * Constructs a PlayerCache instance for the specified player.
     *
     * @param player The player associated with this cache.
     */
    public PlayerCache(Player player) {
        this._player = player;
        this._isGUIOpened = false;
        this._mainMenu = null;
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
}