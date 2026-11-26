package com.github.TavstalDev.rebus.services.box;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.cryptomorin.xseries.XBlock;
import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.config.models.ParticleConfig;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.models.SoundData;
import com.github.TavstalDev.rebus.services.prize.IPrizeService;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import com.github.TavstalDev.yggraLib.api.models.YggraLocation;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemSerializer;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.services.ISoundService;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import com.github.TavstalDev.yggraLib.shadow.yggraParticle.api.IParticle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

@Slf4j
public class BoxService implements IBoxService {
    private final Rebus plugin;
    private final YggraLogger logger;
    private final ITranslationService translator;
    private final IItemSerializer itemSerializer;
    private final IItemStackAccessor itemStackAccessor;
    private final INbtAccessor nbtAccessor;
    private final IScheduler scheduler;
    private final IPrizeService prizeService;
    private final IRebusDatabase database;
    private final ProtocolManager protocolManager;
    private final IParticle particleService;
    private final ISoundService soundService;
    @Getter
    private final String chestKey;
    private final File chestsDirectory;

    private Map<String, Chest> chests;

    public Map<String, Chest> getChests() {return Collections.unmodifiableMap(chests);}

    @Override
    public Set<UUID> playersUnlocking() {
        return playersUnlocking;
    }

    @Override
    public Set<Location> occupiedLocations() {
        return chestsUnderUnlocking;
    }

    public @Nullable Chest getChest(String key) {return chests.get(key);}

    /** Players currently in the unlocking process. */
    public final Set<UUID> playersUnlocking = new HashSet<>();
    /** Locations where chests are currently being unlocked. */
    public final Set<Location> chestsUnderUnlocking = new HashSet<>();

    public BoxService(Rebus plugin, ITranslationService translator, IItemSerializer itemSerializer,
                      IItemStackAccessor itemStackAccessor, INbtAccessor nbtAccessor,
                      IScheduler scheduler, IPrizeService prizeService, IRebusDatabase database,
                      ProtocolManager protocolManager, IParticle particleService, ISoundService soundService) {
        this.plugin = plugin;
        this.logger = plugin.logger().withModule(BoxService.class);
        this.translator = translator;
        this.itemSerializer = itemSerializer;
        this.itemStackAccessor = itemStackAccessor;
        this.nbtAccessor = nbtAccessor;
        this.scheduler = scheduler;
        this.prizeService = prizeService;
        this.database = database;
        this.protocolManager = protocolManager;
        this.particleService = particleService;
        this.soundService = soundService;
        this.chestKey = "rebus_chest";
        this.chests = new LinkedHashMap<>();
        this.chestsDirectory = new File(plugin.getDataFolder(), "chests");
    }

    public void load() {
        // Initialize the set of chests.
        chests = new LinkedHashMap<>();

        if (!chestsDirectory.exists())
            //noinspection ResultOfMethodCallIgnored
            chestsDirectory.mkdirs();

        // save default chests if the directory is empty
        File[] fileList = chestsDirectory.listFiles();
        if (fileList == null || fileList.length == 0) {
            plugin.saveResource("chests/daily.yml", false);
            plugin.saveResource("chests/default.yml", false);
            plugin.saveResource("chests/pandora.yml", false);
            plugin.saveResource("chests/choosen.yml", false);
        }

        // Load all chest configurations from the chests directory.
        for (File file : Objects.requireNonNull(chestsDirectory.listFiles())) {
            String fileName = file.getName();
            if (!fileName.endsWith(".yml"))
                continue;

            YamlConfiguration yaml = new YamlConfiguration();
            try {
                yaml.load(file);

                ConfigurationSection dataSection = yaml.getConfigurationSection("data");
                if (dataSection == null) {
                    logger.warn("No data section found in " + fileName);
                    continue;
                }

                Chest chest = Chest.fromConfigurationSection(fileName.substring(0, fileName.indexOf(".")), dataSection,
                        itemSerializer, logger);
                if (chest != null) {
                    chests.put(chest.getKey(), chest);
                } else {
                    logger.warn("Failed to create chest from data in file: " + fileName);
                }
            } catch (Exception ex) {
                logger.error(String.format("Failed to load '%s' chest file.", file.getPath()), ex);
            }
        }
    }

    public void giveChest(Player player, Chest chest, int amount) {

        ItemStack item = chest.getTranslatedDisplayItem(player, translator, itemStackAccessor, nbtAccessor,
                false);
        if (amount > item.getMaxStackSize())
            amount = item.getMaxStackSize();

        final int finalAmount = amount;
        final UUID playerId = player.getUniqueId();
        scheduler.runAsync(new YggraTask() {
            @Override
            public void run() {
                synchronized (plugin.getPlayerLock(playerId)) {
                    try {
                        RebusConfig config = plugin.config();
                        String chestKey = chest.getKey();
                        ChestUsage chestUsage = database.getUsage(playerId, config.storage.getContext(), chestKey);
                        if (chestUsage == null) {
                            database.chestUsages().add(new ChestUsage(UUID.randomUUID(), player.getUniqueId(),
                                    config.storage.getContext(), chestKey, finalAmount));
                        } else {
                            chestUsage.updateUsages(chestUsage.getUsages() + finalAmount);
                            database.chestUsages().update(chestUsage.getId(), chestUsage);
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to update player chest usages for " + player.getName(), ex);
                    }
                }
            }
        });

        item.setAmount(finalAmount);
        nbtAccessor.set(item, chestKey, chest.getKey());
        player.getInventory().addItem(item);
    }

    public void handlePlaceChest(final Player player, final Chest chest, final ItemStack item, final Block block) {
        // Get the location of the block where the chest is being placed.
        final Location location = block.getLocation();
        final YggraLocation yloc = new YggraLocation(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        chestsUnderUnlocking.add(location);
        playersUnlocking.add(player.getUniqueId());

       removeOneChestItem(player, chest.getKey());

        //#region Animate

        // Temporarily set the block to the chest material and play initial particle effects.
        setTemporaryBlock(player, chest.getBlockMaterial(), block);
        particleService.spawn(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(),
                10, 0.3, 0.3, 0.3, 0.1);

        // Create a new task to handle the animation phases.
        scheduler.runRepeating(new YggraTask() {
            int phase = 0;

            @Override
            public void run() {
                switch (this.phase) {
                    case 0: {
                        // Play the chest opening sound and particle effects.
                        playSound(location, chest.getOpenSound());
                        particleService.spawn(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 20, 0.5, 0.5, 0.5, 0.15);
                        scheduler.runLater(new YggraTask() {
                            @Override
                            public void run() {
                                // Play the chest opening animation.
                                playChestAnimation(block, true);
                            }
                        }, 1L);
                        break;
                    }
                    case 1: {
                        // Play additional particle effects at a lower height.
                        particleService.spawn(location.clone().add(0.5, 0.8, 0.5), chest.getParticle(), 15, 0.3, 0.3, 0.3, 0.1);
                        break;
                    }
                    case 2: {
                        // Play more particle effects and a firework effect if the chest is high-tier.
                        particleService.spawn(location.clone().add(0.5, 1.2, 0.5), chest.getParticle(), 25, 0.6, 0.6, 0.6, 0.2);
                        if (!chest.isHighTier())
                            break;
                        particleService.spawn(location.clone().add(0.5, 1.5, 0.5), plugin.config().particle.getHightierA(), 10, 0.8, 0.8, 0.8, 0.3);
                        break;
                    }
                    case 3: {
                        playSound(location, chest.getCloseSound());
                        scheduler.runLater(new YggraTask() {
                            @Override
                            public void run() {
                                playChestAnimation(block, false);
                            }
                        }, 1L, yloc);
                    }
                    case 4: {
                        this.cancel();

                        scheduler.runLater(new YggraTask() {
                            @Override
                            public void run() {
                                playersUnlocking.remove(player.getUniqueId());
                                playCompletionEffects(location, chest);
                                prizeService.reward(player, chest);
                            }
                        }, 10L, yloc);
                    }
                }
                ++this.phase;
            }
        }, 10L, 15L, yloc);

        //#endregion
    }

    private void playSound(Location location, SoundData soundData) {
        if (soundData == null)
            return;
        Optional<Sound> sound = soundService.get(soundData.getName());
        if (!sound.isPresent())
            return;
        soundService.playAt(location, sound.get(), soundData.getVolume(), soundData.getPitch());
    }

    private void playCompletionEffects(final Location location, final Chest chest) {
        particleService.spawn(location.clone().add(0.5, 0.5, 0.5), chest.getParticle(), chest.getParticleCount(), 0.8, 0.8, 0.8, 0.3);
        final YggraLocation yloc = new YggraLocation(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        final ParticleConfig config = plugin.config().particle;

        scheduler.runLater(new YggraTask() {
            @Override
            public void run() {
                playSound(location, chest.getCompletionSound());
                particleService.spawn(location.clone().add(0.5, 1.5, 0.5), config.getHightierA(), 10, 0.2, 0.2, 0.2, 0.1);
                if (chest.isHighTier()) {
                    particleService.spawn(location.clone().add(0.5, 2.0, 0.5), config.getHightierB(), 15, 1.0, 1.0, 1.0, 0.5);
                }
                if (chest.isSpawnLightningStrike()) {
                    location.getWorld().strikeLightningEffect(location);
                }
            }
        }, 1L, yloc);
    }

    private void setTemporaryBlock(final Player player, final Material material, final Block block) {
        final Location blockLoc = block.getLocation();
        final YggraLocation yloc = new YggraLocation(blockLoc.getWorld().getName(),
                blockLoc.getX(), blockLoc.getY(), blockLoc.getZ(), blockLoc.getYaw(), blockLoc.getPitch());
        
        scheduler.run(new YggraTask() {
            @Override
            public void run() {
                block.setType(material);
                block.getState().update(true);
                rotateChestToPlayer(block, player);
            }
        }, yloc);


        scheduler.runLater(new YggraTask() {
            @Override
            public void run() {
                block.setType(Material.AIR);
                chestsUnderUnlocking.remove(block.getLocation());
            }
        }, 70L, yloc);
    }

    private void rotateChestToPlayer(Block block, Player player) {
        Location playerLoc = player.getLocation();
        Location blockLoc = block.getLocation();
        Vector direction = playerLoc.toVector().subtract(blockLoc.toVector()).normalize();
        XBlock.setDirection(block, getDirectionFromVector(direction));
    }

    private BlockFace getDirectionFromVector(org.bukkit.util.Vector direction) {
        double x = direction.getX();
        double z = direction.getZ();

        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    private void playChestAnimation(Block chestBlock, boolean open) {
        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_ACTION);

            Location loc = chestBlock.getLocation();
            packet.getBlockPositionModifier().write(0, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            packet.getBlocks().write(0, chestBlock.getType());

            // The action ID: 1 for chests
            packet.getIntegers().write(0, 1);

            // The action parameter: 1 for open, 0 for close
            packet.getIntegers().write(1, open ? 1 : 0);

            for (Player player : loc.getWorld().getPlayers()) {
                double distance = loc.distance(player.getLocation());
                if (distance > 16)
                    continue;
                protocolManager.sendServerPacket(player, packet);
            }
        } catch (Exception ex) {
            logger.error("Failed to play chest animation.", ex);
        }
    }

    private boolean isChestItem(ItemStack item, String chestKey) {
        if (!item.hasItemMeta())
            return false;
        String key = nbtAccessor.getString(item, getChestKey());
        return Objects.equals(key, chestKey) && chestKey != null;
    }

    private void removeOneChestItem(Player player, String chestKey) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !isChestItem(stack, chestKey)) {
                continue;
            }

            if (stack.getAmount() > 1) {
                stack.setAmount(stack.getAmount() - 1);
                inv.setItem(i, stack);
            } else {
                inv.setItem(i, null);
            }
            return; // Stop after removing 1 item
        }
    }
}
