package io.github.tavstaldev.rebus;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.samjakob.spigui.SpiGUI;
import io.github.tavstaldev.minecorelib.PluginBase;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.core.PluginTranslator;
import io.github.tavstaldev.minecorelib.utils.ItemMetaSerializer;
import io.github.tavstaldev.minecorelib.utils.VersionUtils;
import io.github.tavstaldev.rebus.commands.CommandRebus;
import io.github.tavstaldev.rebus.commands.CommandRebusAdmin;
import io.github.tavstaldev.rebus.database.IDatabase;
import io.github.tavstaldev.rebus.database.MySqlDatabase;
import io.github.tavstaldev.rebus.database.SqlLiteDatabase;
import io.github.tavstaldev.rebus.events.BlockEventListener;
import io.github.tavstaldev.rebus.events.NpcEventListener;
import io.github.tavstaldev.rebus.events.PlayerEventListener;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.NpcManager;
import io.github.tavstaldev.rebus.util.EconomyUtils;
import io.github.tavstaldev.rebus.util.PermissionUtils;
import org.bukkit.Bukkit;

public final class Rebus extends PluginBase {
    public static Rebus Instance;
    private ItemMetaSerializer _itemMetaSerializer;
    private ChestManager _chestManager;
    private NpcManager _npcManager;
    private SpiGUI _spiGUI;
    private ProtocolManager _protocolManager;
    private IDatabase _database;

    public static PluginLogger Logger() {
        return Instance.getCustomLogger();
    }
    public static PluginTranslator Translator() {
        return Instance.getTranslator();
    }
    public static RebusConfig Config(){
        return (RebusConfig) Instance._config;
    }
    public static ItemMetaSerializer ItemSerializer() {
        return Instance._itemMetaSerializer;
    }
    public static ChestManager Chests() {
        return Instance._chestManager;
    }
    public static NpcManager Npcs() {
        return Instance._npcManager;
    }
    public static SpiGUI GUI() {
        return Instance._spiGUI;
    }
    public static ProtocolManager Protocols() {
        return Instance._protocolManager;
    }
    public static IDatabase Database() { return Instance._database; }

    public Rebus() {
        super(true, "https://github.com/TavstalDev/Rebus/releases/latest");
    }

    @Override
    public void onLoad() {
        _protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onEnable() {
        Instance = this;
        _config = new RebusConfig();
        _config.load(); // Fixes a potential issue with config not being loaded before usage
        _translator = new PluginTranslator(this, new String[]{"eng", "hun"});
        _itemMetaSerializer = new ItemMetaSerializer(this);
        _logger.Info(String.format("Loading %s...", getProjectName()));

        if (VersionUtils.isLegacy()) {
            _logger.Error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register Events
        PlayerEventListener.init();
        BlockEventListener.init();
        NpcEventListener.init();

        // Generate config file
        saveDefaultConfig();

        // Load Localizations
        if (!_translator.Load())
        {
            _logger.Error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register economy integration
        _logger.Debug("Hooking into Vault...");
        if (EconomyUtils.setupEconomy()) {
            _logger.Info("Economy plugin found and hooked into Vault.");
        } else {
            _logger.Warn("Economy plugin not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // check if permissions plugin is installed
        if (!PermissionUtils.setupPermissions()) {
            _logger.Info("Permissions plugin with Vault API support was not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        else
        {
            _logger.Info("Permissions plugin found and hooked into Vault.");
        }

        // Check Citizens Plugin
        _logger.Debug("Hooking into Citizens...");
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens"))
        {
            _logger.Info("Citizens found and hooked into it.");
        }
        else
        {
            _logger.Warn("Citizens not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize SpiGUI
        _logger.Debug("Initializing SpiGUI...");
        _spiGUI = new SpiGUI(this);

        // Register Commands
        _logger.Debug("Registering commands...");
        var command = getCommand("rebus");
        if (command != null) {
            command.setExecutor(new CommandRebus());
        }
        command = getCommand("rebusadmin");
        if (command != null) {
            command.setExecutor(new CommandRebusAdmin());
        }

        // Chest Manager
        _logger.Debug("Initializing Chest Manager...");
        _chestManager = new ChestManager();
        _chestManager.load();

        // Initialize NPC Manager
        _logger.Debug("Initializing NPC Manager...");
        _npcManager = new NpcManager();
        _npcManager.loadExistingNPCs();

        // Initialize database based on configuration
        String databaseType = Config().storageType;
        if (databaseType == null) {
            databaseType = "sqlite";
        }
        switch (databaseType.toLowerCase()) {
            case "mysql": {
                _database = new MySqlDatabase();
                break;
            }
            case "sqlite":
            default: {
                _database = new SqlLiteDatabase();
                break;
            }
        }
        _database.load();
        _database.checkSchema();

        _logger.Ok(String.format("%s has been successfully loaded.", getProjectName()));
        if (Config().checkForUpdates) {
            isUpToDate().thenAccept(upToDate -> {
                if (upToDate) {
                    _logger.Ok("Plugin is up to date!");
                } else {
                    _logger.Warn("A new version of the plugin is available: " + getDownloadUrl());
                }
            }).exceptionally(e -> {
                _logger.Error("Failed to determine update status: " + e.getMessage());
                return null;
            });
        }
    }

    @Override
    public void onDisable() {
        if (this._npcManager != null) {
            this._npcManager.shutdown();
        }
        _logger.Info(String.format("%s has been successfully unloaded.", getProjectName()));
    }

    public void reload() {
        _logger.Info(String.format("Reloading %s...", getProjectName()));
        _logger.Debug("Reloading localizations...");
        _translator.Load();
        _logger.Debug("Localizations reloaded.");
        _logger.Debug("Reloading configuration...");
        this._config.load();
        _logger.Debug("Configuration reloaded.");


        _chestManager.load(); // Reload chests
    }
}
