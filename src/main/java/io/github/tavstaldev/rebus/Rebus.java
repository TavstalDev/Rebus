package io.github.tavstaldev.rebus;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import io.github.tavstaldev.rebus.commands.CommandRebus;
import io.github.tavstaldev.rebus.commands.CommandRebusAdmin;
import io.github.tavstaldev.rebus.database.IRebusDatabase;
import io.github.tavstaldev.rebus.database.MySqlDatabase;
import io.github.tavstaldev.rebus.database.PostgreDatabase;
import io.github.tavstaldev.rebus.database.SqLiteDatabase;
import io.github.tavstaldev.rebus.events.BlockEventListener;
import io.github.tavstaldev.rebus.events.EntityEventListener;
import io.github.tavstaldev.rebus.events.PlayerEventListener;
import io.github.tavstaldev.rebus.gui.MainGUI;
import io.github.tavstaldev.rebus.gui.PreviewGUI;
import io.github.tavstaldev.rebus.managers.ChestManager;
import io.github.tavstaldev.rebus.managers.NpcManager;
import io.github.tavstaldev.rebus.managers.PrizeManager;
import io.github.tavstaldev.rebus.managers.economy.IEconomyManager;
import io.github.tavstaldev.rebus.managers.economy.PlayerPointsManager;
import io.github.tavstaldev.rebus.managers.economy.VaultManager;
import io.github.tavstaldev.rebus.models.NpcTrait;
import io.github.tavstaldev.yggra.core.YggraPlugin;
import io.github.tavstaldev.yggra.core.cache.MemoryCache;
import io.github.tavstaldev.yggra.core.cache.RedisCache;
import io.github.tavstaldev.yggra.core.database.QueryCondition;
import io.github.tavstaldev.yggra.core.gui.GuiManager;
import io.github.tavstaldev.yggra.core.scheduler.ITask;
import io.github.tavstaldev.yggra.core.scheduler.YggraTask;
import io.github.tavstaldev.yggra.core.utils.VersionUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class for the Rebus plugin.
 * Extends PluginBase to provide core plugin functionality.
 */
public final class Rebus extends YggraPlugin<RebusConfig> {
    public static Rebus instance;
    private PrizeManager _prizeManager;
    private ChestManager _chestManager;
    private NpcManager _npcManager;
    private ProtocolManager _protocolManager;
    private IEconomyManager _economyManager;
    private ITask autoCleanTask;
    private final Map<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    /**
     * Gets the database instance.
     * @return The database implementation.
     */
    public IRebusDatabase database() {return (IRebusDatabase)_database;}

    /**
     * Gets the prize manager instance.
     * @return The prize manager.
     */
    public PrizeManager prizeManager() {return _prizeManager;}

    /**
     * Gets the chest manager instance.
     * @return The chest manager.
     */
    public ChestManager chestManager() {return _chestManager;}

    /**
     * Gets the NPC manager instance.
     * @return The NPC manager.
     */
    public NpcManager npcManager() {return _npcManager;}

    /**
     * Gets the economy manager instance.
     * @return The economy manager.
     */
    public IEconomyManager economyManager() {return _economyManager;}

    /**
     * Gets the ProtocolLib protocol manager instance.
     * @return The protocol manager.
     */
    public ProtocolManager protocols() {
        return _protocolManager;
    }

    /**
     * Constructor for the Rebus plugin.
     */
    public Rebus() {
        super(27759);
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
    public void onPluginEnable() {
        instance = this;
        _logger.info(String.format("Loading %s...", getName()));

        _config = new RebusConfig(this);
        _config.load(); // Ensure configuration is loaded before usage

        // Check for compatibility with Minecraft versions
        if (!VersionUtils.isAtLeast(1, 20, 4)) {
            _logger.error("The plugin is not compatible with minecraft versions below 1.20.4. Please use a newer version of the game.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load localizations
        if (!_translator.load()) {
            _logger.error("Failed to load localizations... Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register economy integration
        _logger.debug("Setting up economy...");
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            _economyManager = new PlayerPointsManager(this);
        }
        else {
            _economyManager = new VaultManager(this);
        }
        if (!_economyManager.enabled())
            return;

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

        boolean enableRedis = config().storageRedisEnabled;
        if (enableRedis)
            _cache = new RedisCache(config().storageRedisHost, config().storageRedisPort, config().storageRedisUsername, config().storageRedisPassword);
        else
            _cache = new MemoryCache(logger());

        // Initialize PrizeManager
        _logger.debug("Initializing Prize Manager...");
        _prizeManager = new PrizeManager(this);
        _prizeManager.load();

        // Initialize ChestManager
        _logger.debug("Initializing Chest Manager...");
        _chestManager = new ChestManager(this, _prizeManager);
        _chestManager.load();

        // Initialize NpcManager
        _logger.debug("Initializing NPC Manager...");
        _npcManager = new NpcManager(this);

        // Initialize database based on configuration
        String databaseType = config().storageType;
        if (databaseType == null)
            databaseType = "sqlite";
        switch (databaseType.toLowerCase()) {
            case "mysql":
            case "mariadb": {
                try
                {
                    _database = new MySqlDatabase(this);
                }
                catch (Exception ex) {
                    _logger.error("Failed to construct mysql/mariadb database. Unloading...", ex);
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            }
            case "postgre":
            case "postgres":
            case "postgresql": {
                try {
                    _database = new PostgreDatabase(this);
                }
                catch (Exception ex) {
                    _logger.error("Failed to construct postgres database. Unloading...", ex);
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            }
            case "sqlite":
            default: {
                try {
                    _database = new SqLiteDatabase(this);
                }
                catch (Exception ex) {
                    _logger.error("Failed to construct sqlite database. Unloading...", ex);
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            }
        }
        _database.load();
        _database.checkSchema();

        // Register event listeners
        new PlayerEventListener(this, chestManager());
        new BlockEventListener(this, database(), chestManager());
        new EntityEventListener(this, chestManager());

        // Initialize SpiGUI
        _logger.debug("Initializing gui...");
        _gui = new GuiManager(this);
        _gui.register(new MainGUI(this, database(), chestManager(), economyManager()));
        _gui.register(new PreviewGUI(this, prizeManager(), chestManager()));

        // Register commands
        _logger.debug("Registering commands...");
        try {
            var command = getCommand("rebus");
            if (command != null) {
                command.setExecutor(new CommandRebus(this));
            }
            command = getCommand("rebusadmin");
            if (command != null) {
                command.setExecutor(new CommandRebusAdmin(this));
            }
        }
        catch (Exception ex) {
            _logger.error("Failed to register commands.", ex);
        }

        autoCleanTask = scheduler().runRepeatingAsync(getCleanTask(), 60L, _config.storageAutoClean);

        _logger.ok(String.format("%s has been successfully loaded.", getName()));

        // Check for updates if enabled in configuration
        /*if (config().checkForUpdates) {
            isUpToDate().thenAccept(upToDate -> {
                if (upToDate) {
                    _logger.ok("Plugin is up to date!");
                } else {
                    _logger.warn("A new version of the plugin is available: " + getDownloadUrl());
                }
            }).exceptionally(e -> {
                _logger.error("Failed to determine update status: ", e);
                return null;
            });
        }*/
    }

    /**
     * Called when the plugin is disabled.
     * Cleans up resources and shuts down managers.
     */
    @Override
    public void onPluginDisable() {

        // Unregister GUIs
        if (_gui != null) {
            _gui.closeAll();

            _gui.unregister(MainGUI.ID);
            _gui.unregister(PreviewGUI.ID);
            _gui.invalidateAllCaches();
        }

        if (!autoCleanTask.isCancelled())
            autoCleanTask.cancel();

        _logger.info(String.format("%s has been successfully unloaded.", getName()));
    }

    /**
     * Reloads the plugin's configuration and localizations.
     * Also reloads the ChestManager.
     */
    public void reload() {
        _logger.info(String.format("Reloading %s...", getName()));
        _logger.debug("Reloading localizations...");
        _translator.load();
        _logger.debug("Localizations reloaded.");
        _logger.debug("Reloading configuration...");
        this._config.load();
        _logger.debug("Configuration reloaded.");

        if (!autoCleanTask.isCancelled())
            autoCleanTask.cancel();

        assert _database != null;
        _database.unload();
        _database.load();
        _database.checkSchema();

        // Reload prizes
        _prizeManager.load();

        // Reload chests
        _chestManager.load();

        // Re-register GUIs
        if (_gui != null) {
            _gui.closeAll();

            _gui.unregister(MainGUI.ID);
            _gui.unregister(PreviewGUI.ID);
            _gui.invalidateAllCaches();

            _gui.register(new MainGUI(this, database(), chestManager(), economyManager()));
            _gui.register(new PreviewGUI(this, prizeManager(), chestManager()));
        }

        autoCleanTask = scheduler().runRepeatingAsync(getCleanTask(), 60L, _config.storageAutoClean);
    }

    /**
     * Creates a recurring task that deletes expired cooldowns from the database.
     * Cancels the task if the database is unavailable or an error occurs.
     */
    private YggraTask getCleanTask() {
        return new YggraTask() {
            @Override
            public void run() {
                try {
                    if (database() == null)
                    {
                        autoCleanTask.cancel();
                        return;
                    }
                    database().cooldowns().deleteByCriteria(QueryCondition.lt("expiresAt", LocalDateTime.now()));
                }
                catch (Exception ex) {
                    _logger.error("Failed to auto clean database.", ex);
                    autoCleanTask.cancel();
                }
            }
        };
    }

    /**
     * Retrieves or creates a lock object for the given player, used for synchronization.
     *
     * @param playerId The UUID of the player.
     * @return The lock object associated with the player.
     */
    public Object getPlayerLock(UUID playerId) {
        return playerLocks.computeIfAbsent(playerId, k -> new Object());
    }

    /**
     * Removes the lock object associated with the given player.
     *
     * @param playerId The UUID of the player.
     */
    public void removePlayerLock(UUID playerId) {
        playerLocks.remove(playerId);
    }
}