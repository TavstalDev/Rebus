package io.github.tavstaldev.rebus.models;

import com.samjakob.spigui.menu.SGMenu;
import io.github.tavstaldev.rebus.gui.MainGUI;
import io.github.tavstaldev.rebus.gui.PreviewGUI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class PlayerCache {
    private final Player _player;
    private boolean _isGUIOpened;
    private SGMenu _mainMenu;
    private RebusChest _previewChest;
    private Set<ItemStack> _items;
    private SGMenu _previewMenu;
    private int _previewPage;

    public PlayerCache(Player player) {
        this._player = player;
        this._isGUIOpened = false;
        this._mainMenu = null;
        this._previewChest = null;
        this._previewMenu = null;
        this._previewPage = 1;
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

    public SGMenu getPreviewMenu() {
        if (_previewMenu == null) {
            _previewMenu = PreviewGUI.create(_player);
        }
        return _previewMenu;
    }

    public int getPreviewPage() {
        return _previewPage;
    }

    public void setPreviewPage(int page) {
        this._previewPage = page;
    }

    public RebusChest getPreviewChest() {
        return _previewChest;
    }

    public Set<ItemStack> getItems() {
        return _items;
    }

    public void setPreviewChest(RebusChest chest) {
        this._previewChest = chest;
    }
}
