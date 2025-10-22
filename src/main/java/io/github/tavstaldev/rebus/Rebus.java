package io.github.tavstaldev.rebus;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.samjakob.spigui.SpiGUI;
import io.github.tavstaldev.banyaszLib.api.BanyaszApi;
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
import io.github.tavstaldev.rebus.events.PlayerEventListener;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.NpcManager;
import io.github.tavstaldev.rebus.tasks.CacheCleanTask;
import org.bukkit.Bukkit;

public final class Rebus extends PluginBase {
    public static Rebus Instance;
    private ItemMetaSerializer itemMetaSerializer;
    private ChestManager chestManager;
    private NpcManager npcManager;
    private SpiGUI spiGUI;
    private ProtocolManager protocolManager;
    private IDatabase database;
    private BanyaszApi banyaszApi;
    private CacheCleanTask cacheCleanTask; // Task for cleaning player caches.


    public static PluginLogger logger() {
        return Instance.getCustomLogger();
    }
    public static PluginTranslator translator() {
        return Instance.getTranslator();
    }
    public static RebusConfig config(){
        return (RebusConfig) Instance._config;
    }
    public static ItemMetaSerializer itemSerializer() {
        return Instance.itemMetaSerializer;
    }
    public static ChestManager chestManager() {
        return Instance.chestManager;
    }
    public static NpcManager npcManager() {
        return Instance.npcManager;
    }
    public static SpiGUI gui() {
        return Instance.spiGUI;
    }
    public static ProtocolManager protocols() {
        return Instance.protocolManager;
    }
    public static IDatabase database() { return Instance.database; }
    public static BanyaszApi banyaszApi() { return Instance.banyaszApi; }

    public Rebus() {
        super(true, "https://github.com/TavstalDev/Rebus/releases/latest");
    }

    @Override
    public void onLoad() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onEnable() {
        Instance = this;
        super.onEnable(); // call parent method
        _config = new RebusConfig();
        _config.load(); // Fixes a potential issue with config not being loaded before usage
        _translator = new PluginTranslator(this, new String[]{"eng", "hun"});
        itemMetaSerializer = new ItemMetaSerializer(this);
        _logger.info(String.format("Loading %s...", getProjectName()));

        if (VersionUtils.isLegacy()) {
            _logger.error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register Events
        PlayerEventListener.init();
        BlockEventListener.init();

        // Load Localizations
        if (!_translator.load())
        {
            _logger.error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check BanyaszLib Plugin
        _logger.debug("Hooking into BanyaszLib...");
        if (Bukkit.getPluginManager().isPluginEnabled("BanyaszLib")) {
            banyaszApi = BanyaszApi.getInstance();
            _logger.info("BanyaszLib found and hooked into it.");
        } else {
            _logger.warn("BanyaszLib not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check Citizens Plugin
        _logger.debug("Hooking into Citizens...");
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens"))
        {
            _logger.info("Citizens found and hooked into it.");
        }
        else
        {
            _logger.warn("Citizens not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize SpiGUI
        _logger.debug("Initializing SpiGUI...");
        spiGUI = new SpiGUI(this);

        // Register Commands
        _logger.debug("Registering commands...");
        var command = getCommand("rebus");
        if (command != null) {
            command.setExecutor(new CommandRebus());
        }
        command = getCommand("rebusadmin");
        if (command != null) {
            command.setExecutor(new CommandRebusAdmin());
        }

        // Chest Manager
        _logger.debug("Initializing Chest Manager...");
        chestManager = new ChestManager();
        chestManager.load();

        // Initialize NPC Manager
        _logger.debug("Initializing NPC Manager...");
        npcManager = new NpcManager();

        // Initialize database based on configuration
        String databaseType = config().storageType;
        if (databaseType == null) {
            databaseType = "sqlite";
        }
        switch (databaseType.toLowerCase()) {
            case "mysql": {
                database = new MySqlDatabase();
                break;
            }
            case "sqlite":
            default: {
                database = new SqlLiteDatabase();
                break;
            }
        }
        database.load();
        database.checkSchema();

        // Register cache cleanup task.
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();

        cacheCleanTask = new CacheCleanTask(); // Runs every 5 minutes
        cacheCleanTask.runTaskTimer(this, 0, 5 * 60 * 20);

        _logger.ok(String.format("%s has been successfully loaded.", getProjectName()));
        if (config().checkForUpdates) {
            isUpToDate().thenAccept(upToDate -> {
                if (upToDate) {
                    _logger.ok("Plugin is up to date!");
                } else {
                    _logger.warn("A new version of the plugin is available: " + getDownloadUrl());
                }
            }).exceptionally(e -> {
                _logger.error("Failed to determine update status: " + e.getMessage());
                return null;
            });
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        _logger.info(String.format("%s has been successfully unloaded.", getProjectName()));
    }

    public void reload() {
        _logger.info(String.format("Reloading %s...", getProjectName()));
        _logger.debug("Reloading localizations...");
        _translator.load();
        _logger.debug("Localizations reloaded.");
        _logger.debug("Reloading configuration...");
        this._config.load();
        _logger.debug("Configuration reloaded.");

        database.unload();
        database.load();
        database.checkSchema();

        chestManager.load(); // Reload chests
    }
}
