package io.github.tavstaldev.rebus.models;

import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

public class PlayerCache {
    private final Player _player;
    private boolean _isGUIOpened;
    private SGMenu _mainMenu;
    private Set<Cooldown> _cooldowns;

    public PlayerCache(Player player) {
        this._player = player;
        this._isGUIOpened = false;
        this._mainMenu = null;
        this._cooldowns = Rebus.Database().getCooldowns(player.getUniqueId());
    }

    public boolean isGuiOpened() {
        return _isGUIOpened;
    }

    public void setGuiOpened(boolean isGUIOpened) {
        this._isGUIOpened = isGUIOpened;
    }

    public SGMenu getMainMenu() {
        if (_mainMenu == null) {
            _mainMenu = MainGUI.create(_player);
        }
        return _mainMenu;
    }

    public Set<Cooldown> getCooldowns() {
        return _cooldowns;
    }

    public boolean isUnderCooldown(RebusChest chest) {
        final String context = Rebus.Config().storageContext;
        for (Cooldown cooldown : _cooldowns) {
            if (cooldown.getContext().equals(context) && !cooldown.isExpired() && cooldown.getChest().equals(chest.getKey())) {
                return true;
            }
        }
        return false;
    }

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
