package com.github.TavstalDev.rebus;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.github.TavstalDev.rebus.commands.CommandRebus;
import com.github.TavstalDev.rebus.commands.CommandRebusAdmin;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.config.models.RedisConfig;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.database.MySqlDatabase;
import com.github.TavstalDev.rebus.database.PostgreDatabase;
import com.github.TavstalDev.rebus.database.SqLiteDatabase;
import com.github.TavstalDev.rebus.events.BlockEventListener;
import com.github.TavstalDev.rebus.events.EntityEventListener;
import com.github.TavstalDev.rebus.events.PlayerEventListener;
import com.github.TavstalDev.rebus.gui.MainGUI;
import com.github.TavstalDev.rebus.gui.PreviewGUI;
import com.github.TavstalDev.rebus.models.NpcTrait;
import com.github.TavstalDev.rebus.services.box.BoxService;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import com.github.TavstalDev.rebus.services.economy.IEconomyService;
import com.github.TavstalDev.rebus.services.economy.PlayerPointsService;
import com.github.TavstalDev.rebus.services.economy.VaultEconomyService;
import com.github.TavstalDev.rebus.services.nbt.NbtAccessor;
import com.github.TavstalDev.rebus.services.npc.INpcService;
import com.github.TavstalDev.rebus.services.npc.NpcService;
import com.github.TavstalDev.rebus.services.prize.IPrizeService;
import com.github.TavstalDev.rebus.services.prize.PrizeService;
import com.github.TavstalDev.yggraLib.api.cache.ICache;
import com.github.TavstalDev.yggraLib.api.cache.impl.MemoryCache;
import com.github.TavstalDev.yggraLib.api.cache.impl.RedisCache;
import com.github.TavstalDev.yggraLib.api.database.QueryCondition;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.ITask;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.bootstrap.YggraPlugin;
import com.github.TavstalDev.yggraLib.bootstrap.models.LibraryInfo;
import com.github.TavstalDev.yggraLib.bukkit.gui.GuiService;
import com.github.TavstalDev.yggraLib.bukkit.gui.IGuiService;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemSerializer;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import com.github.TavstalDev.yggraLib.bukkit.services.ISoundService;
import com.github.TavstalDev.yggraLib.bukkit.utils.VersionUtils;
import com.github.TavstalDev.yggraLib.shadow.yggraParticle.api.IParticle;
import com.github.TavstalDev.yggraLib.shadow.yggraTitle.api.ITitle;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;

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
    public final static PluginManager pluginManager = Bukkit.getPluginManager();
    private ITask autoCleanTask;
    private final Map<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    @Override
    public String downloadUrl() {
        return "https://api.github.com/repos/TavstalDev/rebus/releases/latest";
    }

    /**
     * Constructor for the Rebus plugin.
     */
    public Rebus() {
        super(27759,
                new LibraryInfo[]{
                        new LibraryInfo(
                                "com{}zaxxer",
                                "HikariCP",
                                "4.0.3",
                                null
                        )
                }
        );
    }

    @Override
    public boolean onPrePluginEnable() {
        instance = this;
        _config = new RebusConfig(this);
        _config.load();

        if (!pluginManager.isPluginEnabled("ProtocolLib")) {
            _logger.error("ProtocolLib is not installed, unloading...");
            pluginManager.disablePlugin(this);
            return false;
        }

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager != null)
            _serviceCollection.put(ProtocolManager.class, protocolManager);

        _serviceCollection.put(INbtAccessor.class, new NbtAccessor());
        return true;
    }

    @Override
    public void onPluginEnable() {
        _logger.info("Loading...");
        if (!VersionUtils.isAtLeast(1, 8)) {
            _logger.error("The plugin is not compatible with legacy versions of Minecraft. Please use a newer version of the game.");
            pluginManager.disablePlugin(this);
            return;
        }

        _config = new RebusConfig(this);
        _config.load(); // Ensure configuration is loaded before usage

        ITranslationService translator = getService(ITranslationService.class);
        if (translator == null) {
            _logger.error("Failed to load localizations... Unloading...");
            pluginManager.disablePlugin(this);
            return;
        }

        // Register economy integration
        _logger.debug("Setting up economy...");
        IEconomyService economyService = pluginManager.isPluginEnabled("PlayerPoints") ?
                new PlayerPointsService(this) : new VaultEconomyService(this);

        _serviceCollection.put(IEconomyService.class, economyService);
        if (!economyService.enabled())
            return;

        // Check for Citizens plugin
        _logger.debug("Hooking into Citizens...");
        if (!pluginManager.isPluginEnabled("Citizens")) {
            _logger.warn("Citizens not found. Unloading...");
            pluginManager.disablePlugin(this);
            return;
        } else {
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(NpcTrait.class));
            _logger.info("Citizens found and hooked into it.");
        }

        // Setup cache
        ICache cacheService;
        RedisConfig redis = config().storage.getRedis();
        if (redis.isEnabled()) {
            cacheService = new RedisCache(
                    redis.getHost(),
                    redis.getPort(),
                    redis.getUsername(),
                    redis.getPassword()
            );
        } else {
            cacheService = new MemoryCache(logger().withModule(MemoryCache.class));
        }
        _serviceCollection.put(ICache.class, cacheService);

        // Initialize database based on configuration
        IRebusDatabase database;
        String databaseType = config().storage.getType();
        if (databaseType == null)
            databaseType = "sqlite";
        switch (databaseType.toLowerCase()) {
            case "mysql":
            case "mariadb": {
                try
                {
                    database = new MySqlDatabase(this);
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
                    database = new PostgreDatabase(this);
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
                    database = new SqLiteDatabase(this);
                }
                catch (Exception ex) {
                    _logger.error("Failed to construct sqlite database. Unloading...", ex);
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            }
        }
        database.load();
        database.checkSchema();
        _serviceCollection.put(IRebusDatabase.class, database);

        // Initialize PrizeManager
        _logger.debug("Initializing Prize Manager...");
        IItemSerializer itemSerializer = getService(IItemSerializer.class);
        IItemStackAccessor itemStackAccessor = getService(IItemStackAccessor.class);
        IChatService chatService = getService(IChatService.class);
        ITitle titleService = getService(ITitle.class);
        IScheduler scheduler = getService(IScheduler.class);

        IPrizeService prizeService = new PrizeService(this, translator,
                itemSerializer, itemStackAccessor, chatService, titleService, database, scheduler);
        _serviceCollection.put(IPrizeService.class, prizeService);
        prizeService.load();

        // Initialize ChestManager
        _logger.debug("Initializing Chest Manager...");
        INbtAccessor nbtAccessor = getService(INbtAccessor.class);
        ProtocolManager protocolManager = getService(ProtocolManager.class);
        ISoundService soundService = getService(ISoundService.class);
        IParticle particleService = getService(IParticle.class);

        IBoxService boxService = new BoxService(this, translator, itemSerializer, itemStackAccessor, nbtAccessor,
                scheduler, prizeService, database, protocolManager, particleService, soundService);
        _serviceCollection.put(IBoxService.class, boxService);
        boxService.load();

        // Initialize NpcManager
        _logger.debug("Initializing NPC Manager...");
        _serviceCollection.put(INpcService.class, new NpcService(this, chatService));

        // Register event listeners
        new PlayerEventListener(this, boxService);
        new BlockEventListener(this, translator, chatService, scheduler, database, boxService, nbtAccessor);
        new EntityEventListener(this, boxService);

        // Initialize SpiGUI
        _logger.debug("Initializing gui...");
        IGuiService guiService = new GuiService(this, cacheService);
        _serviceCollection.put(IGuiService.class, guiService);
        guiService.register(new MainGUI(this, translator, chatService, scheduler, itemStackAccessor,
                nbtAccessor, guiService, economyService, database, boxService));
        guiService.register(new PreviewGUI(this, translator, itemStackAccessor, nbtAccessor,
                guiService, prizeService, boxService));

        // Register commands
        _logger.debug("Registering commands...");
        try {
            PluginCommand command = getCommand("rebus");
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

        assert scheduler != null;
        autoCleanTask = scheduler.runRepeatingAsync(getCleanTask(), 60L, _config.storage.getAutoClean());

        _logger.ok(String.format("%s has been successfully loaded.", getName()));

        // Check for updates if enabled in configuration
        if (config().general.isCheckForUpdates()) {
            isUpToDate().thenAccept(upToDate -> {
                if (upToDate == null)
                    return;

                if (upToDate) {
                    _logger.ok("Plugin is up to date!");
                    return;
                }

                _logger.warn("A new version of the plugin is available at: " + downloadUrl());
            }).exceptionally(e -> {
                _logger.error("Failed to determine update status: " + e.toString());
                return null;
            });
        }
    }

    /**
     * Called when the plugin is disabled.
     * Cleans up resources and shuts down managers.
     */
    @Override
    public void onPluginDisable() {
        if (autoCleanTask != null && !autoCleanTask.isCancelled())
            autoCleanTask.cancel();
    }

    /**
     * Reloads the plugin's configuration and localizations.
     * Also reloads the ChestManager.
     */
    public void reload() {
        if (VersionUtils.isPaper() || VersionUtils.isFolia()) {
            onPrePluginDisable();
            onPluginDisable();
            if (onPrePluginEnable())
                onPluginEnable();
            return;
        }

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.disablePlugin(this);
        pluginManager.enablePlugin(this);
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
                    IRebusDatabase database = getService(IRebusDatabase.class);
                    if (database == null)
                    {
                        autoCleanTask.cancel();
                        return;
                    }
                    database.cooldowns().deleteByCriteria(QueryCondition.lt("expiresAt", LocalDateTime.now()));
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