package io.github.tavstaldev.rebus.models;

import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    // A set of cooldowns for the player.
    private Set<Cooldown> _cooldowns;

    // A set of buy cooldowns for the player.
    private Set<Cooldown> _buyCooldowns;

    /**
     * Constructs a PlayerCache instance for the specified player.
     *
     * @param player The player associated with this cache.
     */
    public PlayerCache(Player player) {
        this._player = player;
        this._isGUIOpened = false;
        this._mainMenu = null;
        this._cooldowns = Rebus.Database().getCooldowns(player.getUniqueId());
        this._buyCooldowns = new HashSet<>();
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
     * Retrieves the set of cooldowns for the player.
     *
     * @return A set of cooldowns.
     */
    public Set<Cooldown> getCooldowns() {
        return _cooldowns;
    }

    /**
     * Adds a buy cooldown for the specified chest.
     *
     * @param chest The chest for which to add a buy cooldown.
     */
    public void addBuyCooldown(RebusChest chest) {
        final String context = Rebus.Config().storageContext;
        _buyCooldowns.removeIf(cooldown -> cooldown.getContext().equals(context) && cooldown.getChest().equals(chest.getKey()));
        Cooldown newCooldown = new Cooldown(Rebus.Config().storageContext, chest.getKey(), LocalDateTime.now().plusSeconds(chest.getCooldown()));
        _buyCooldowns.add(newCooldown);
    }

    /**
     * Retrieves the remaining buy cooldown time for the specified chest.
     *
     * @param chest The chest for which to retrieve the buy cooldown.
     * @return The remaining cooldown time in seconds, or 0 if no cooldown exists.
     */
    public long getBuyCooldown(RebusChest chest) {
        final String context = Rebus.Config().storageContext;
        for (Cooldown cooldown : _buyCooldowns) {
            if (cooldown.getContext().equals(context) && !cooldown.isExpired() && cooldown.getChest().equals(chest.getKey())) {
                return Duration.between(LocalDateTime.now(), cooldown.getExpiresAt()).abs().toSeconds();
            }
        }
        return 0;
    }

    /**
     * Retrieves the remaining cooldown time for the specified chest.
     *
     * @param chest The chest for which to retrieve the cooldown.
     * @return The remaining cooldown time in seconds, or 0 if no cooldown exists.
     */
    public long getCooldown(RebusChest chest) {
        final String context = Rebus.Config().storageContext;
        for (Cooldown cooldown : _cooldowns) {
            if (cooldown.getContext().equals(context) && !cooldown.isExpired() && cooldown.getChest().equals(chest.getKey())) {
                return Duration.between(LocalDateTime.now(), cooldown.getExpiresAt()).abs().toSeconds();
            }
        }
        return 0;
    }
}