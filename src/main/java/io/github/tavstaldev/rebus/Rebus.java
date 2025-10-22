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
import io.github.tavstaldev.rebus.events.PlayerEventListener;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.NpcManager;
import io.github.tavstaldev.rebus.models.NpcTrait;
import io.github.tavstaldev.rebus.tasks.CacheCleanTask;
import io.github.tavstaldev.rebus.util.EconomyUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;

/**
 * Main class for the Rebus plugin.
 * Extends PluginBase to provide core plugin functionality.
 */
public final class Rebus extends PluginBase {
    // Singleton instance of the plugin
    public static Rebus Instance;

    // Various plugin components and managers
    private ItemMetaSerializer _itemMetaSerializer;
    private ChestManager _chestManager;
    private NpcManager _npcManager;
    private SpiGUI _spiGUI;
    private ProtocolManager _protocolManager;
    private IDatabase _database;
    private CacheCleanTask cacheCleanTask; // Task for cleaning player caches.

    /**
     * Provides access to the plugin's logger.
     * @return PluginLogger instance.
     */
    public static PluginLogger logger() {
        return Instance.getCustomLogger();
    }

    /**
     * Provides access to the plugin's translator.
     * @return PluginTranslator instance.
     */
    public static PluginTranslator translator() {
        return Instance.getTranslator();
    }

    /**
     * Provides access to the plugin's configuration.
     * @return RebusConfig instance.
     */
    public static RebusConfig config(){
        return (RebusConfig) Instance._config;
    }

    /**
     * Provides access to the ItemMetaSerializer.
     * @return ItemMetaSerializer instance.
     */
    public static ItemMetaSerializer itemSerializer() {
        return Instance._itemMetaSerializer;
    }

    /**
     * Provides access to the ChestManager.
     * @return ChestManager instance.
     */
    public static ChestManager chestManager() {
        return Instance._chestManager;
    }

    /**
     * Provides access to the NpcManager.
     * @return NpcManager instance.
     */
    public static NpcManager npcManager() {
        return Instance._npcManager;
    }

    /**
     * Provides access to the SpiGUI instance.
     * @return SpiGUI instance.
     */
    public static SpiGUI gui() {
        return Instance._spiGUI;
    }

    /**
     * Provides access to the ProtocolManager.
     * @return ProtocolManager instance.
     */
    public static ProtocolManager protocols() {
        return Instance._protocolManager;
    }

    /**
     * Provides access to the database instance.
     * @return IDatabase instance.
     */
    public static IDatabase database() {
        return Instance._database;
    }

    /**
     * Constructor for the Rebus plugin.
     * Initializes the plugin with update checking enabled and a default download URL.
     */
    public Rebus() {
        super(true, "https://github.com/TavstalDev/Rebus/releases/latest");
    }

    /**
     * Called when the plugin is loaded.
     * Initializes the ProtocolManager.
     */
    @Override
    public void onLoad() {
        _protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Called when the plugin is enabled.
     * Initializes various components, checks dependencies, and sets up the plugin.
     */
    @Override
    public void onEnable() {
        Instance = this;
        super.onEnable(); // Call parent method
        _config = new RebusConfig();
        _config.load(); // Ensure configuration is loaded before usage
        _translator = new PluginTranslator(this, new String[]{"eng", "hun"});
        _itemMetaSerializer = new ItemMetaSerializer(this);
        _logger.info(String.format("Loading %s...", getProjectName()));

        // Check for compatibility with Minecraft versions
        if (VersionUtils.isLegacy()) {
            _logger.error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        PlayerEventListener.init();
        BlockEventListener.init();

        // Load localizations
        if (!_translator.load()) {
            _logger.error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register economy integration
        _logger.debug("Hooking into Vault...");
        if (!EconomyUtils.setupEconomy()) {
            _logger.warn("Economy plugin not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            _logger.info("Economy plugin found and hooked into Vault.");
        }

        // Check for Citizens plugin
        _logger.debug("Hooking into Citizens...");
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            _logger.warn("Citizens not found. Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(NpcTrait.class));
            _logger.info("Citizens found and hooked into it.");
        }

        // Initialize SpiGUI
        _logger.debug("Initializing SpiGUI...");
        _spiGUI = new SpiGUI(this);

        // Register commands
        _logger.debug("Registering commands...");
        var command = getCommand("rebus");
        if (command != null) {
            command.setExecutor(new CommandRebus());
        }
        command = getCommand("rebusadmin");
        if (command != null) {
            command.setExecutor(new CommandRebusAdmin());
        }

        // Initialize ChestManager
        _logger.debug("Initializing Chest Manager...");
        _chestManager = new ChestManager();
        _chestManager.load();

        // Initialize NpcManager
        _logger.debug("Initializing NPC Manager...");
        _npcManager = new NpcManager();

        // Initialize database based on configuration
        String databaseType = config().storageType;
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

        // Register cache cleanup task.
        if (cacheCleanTask != null && !cacheCleanTask.isCancelled())
            cacheCleanTask.cancel();
        cacheCleanTask = new CacheCleanTask(); // Runs every 5 minutes
        cacheCleanTask.runTaskTimer(this, 0, 5 * 60 * 20);

        _logger.ok(String.format("%s has been successfully loaded.", getProjectName()));

        // Check for updates if enabled in configuration
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

    /**
     * Called when the plugin is disabled.
     * Cleans up resources and shuts down managers.
     */
    @Override
    public void onDisable() {
        super.onDisable();
        _logger.info(String.format("%s has been successfully unloaded.", getProjectName()));
    }

    /**
     * Reloads the plugin's configuration and localizations.
     * Also reloads the ChestManager.
     */
    public void reload() {
        _logger.info(String.format("Reloading %s...", getProjectName()));
        _logger.debug("Reloading localizations...");
        _translator.load();
        _logger.debug("Localizations reloaded.");
        _logger.debug("Reloading configuration...");
        this._config.load();
        _logger.debug("Configuration reloaded.");

        _database.unload();
        _database.load();
        _database.checkSchema();

        // Reload chests
        _chestManager.load();
    }
}