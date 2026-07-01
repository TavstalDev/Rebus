package io.github.tavstaldev.rebus.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.database.models.ChestUsage;
import io.github.tavstaldev.rebus.models.Chest;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import io.github.tavstaldev.yggra.core.scheduler.YggraTask;
import io.github.tavstaldev.yggra.items.api.utils.TypeUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Manages chest definitions, placement, animations, and the rewarding process.
 */
public class ChestManager {

    private final Rebus plugin;
    private final YggraLogger logger;
    private final PrizeManager prizeManager;
    private final NamespacedKey chestKey;
    private final File chestsDirectory;
    private final Yaml yamlParser;

    /**
     * Gets the namespaced key used to identify chest items.
     *
     * @return The chest namespaced key.
     */
    public NamespacedKey getChestKey() {
        return chestKey;
    }

    private Map<String, Chest> chests;

    /**
     * Returns an unmodifiable view of all loaded chests.
     *
     * @return A map of chest keys to chest instances.
     */
    public Map<String, Chest> getChests() {return Collections.unmodifiableMap(chests);}

    /**
     * Gets a chest by its key.
     *
     * @param key The chest key.
     * @return The chest, or null if not found.
     */
    public @Nullable Chest getChest(String key) {return chests.get(key);}

    /** Players currently in the unlocking process. */
    public final Set<UUID> playersUnlocking = new HashSet<>();
    /** Locations where chests are currently being unlocked. */
    public final Set<Location> chestsUnderUnlocking = new HashSet<>();

    /**
     * Creates a new chest manager for the given plugin.
     *
     * @param plugin       The plugin instance.
     * @param prizeManager The prize manager for handling rewards.
     */
    public ChestManager(Rebus plugin, PrizeManager prizeManager) {
        this.plugin = plugin;
        this.logger = plugin.logger().withModule(ChestManager.class);
        this.prizeManager = prizeManager;
        this.chestKey = new NamespacedKey(plugin, "rebus_chest");
        this.chests = new LinkedHashMap<>();
        this.chestsDirectory = new File(plugin.getDataFolder(), "chests");

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        this.yamlParser = new Yaml(options);
    }

    /**
     * Loads all chest configurations from the chests directory.
     */
    public void load() {
        // Initialize the set of chests.
        chests = new LinkedHashMap<>();

        if (!chestsDirectory.exists())
            //noinspection ResultOfMethodCallIgnored
            chestsDirectory.mkdirs();

        // save default chests if the directory is empty
        var fileList = chestsDirectory.listFiles();
        if (fileList == null || fileList.length == 0) {
            plugin.saveResource("chests/daily.yml", false);
            plugin.saveResource("chests/default.yml", false);
            plugin.saveResource("chests/pandora.yml", false);
            plugin.saveResource("chests/choosen.yml", false);
        }

        // Load all chest configurations from the chests directory.
        for (File file : Objects.requireNonNull(chestsDirectory.listFiles())) {
            String fileName = file.getName();
            if (!fileName.endsWith(".yml")) continue;
            try (FileInputStream stream = new FileInputStream(file)) {
                Map<String, Object> yamlMap = yamlParser.load(stream);

                if (yamlMap == null || !yamlMap.containsKey("data")) {
                    logger.warn("Invalid chest data in file: " + fileName);
                    continue;
                }

                Map<String, Object> dataMap = TypeUtils.castAsMap(yamlMap.get("data"), null);
                if (dataMap == null) {
                    logger.warn("Invalid data section in chest file: " + fileName);
                    continue;
                }

                var chest = Chest.fromMap(fileName.substring(0, fileName.indexOf(".")), dataMap, plugin.itemSerializer(), logger);
                if (chest != null) {
                    chests.put(chest.getKey(), chest);
                    logger.info("Loaded chest: " + chest.getKey() + " from file: " + fileName);
                } else {
                    logger.warn("Failed to create chest from data in file: " + fileName);
                }
            }
            catch (Exception ex) {
                logger.error("Failed to load chest from file: " + fileName, ex);
            }
        }
    }

    /**
     * Gives a chest item to a player and updates their usage count in the database.
     *
     * @param player The player to give the chest to.
     * @param chest  The chest type to give.
     * @param amount The number of uses to grant.
     */
    public void giveChest(Player player, Chest chest, int amount) {

        ItemStack item = chest.getTranslatedDisplayItem(player, plugin.translator(), false);
        if (amount > item.getMaxStackSize())
            amount = item.getMaxStackSize();

        final int finalAmount = amount;
        final UUID playerId = player.getUniqueId();
        plugin.scheduler().runAsync(new YggraTask() {
            @Override
            public void run() {
                synchronized (plugin.getPlayerLock(playerId)) {
                    try {
                        var config = plugin.config();
                        var database = plugin.database();
                        var chestKey = chest.getKey();
                        var chestUsage = database.getUsage(playerId, config.storageContext, chestKey);
                        if (chestUsage == null) {
                            database.chestUsages().add(new ChestUsage(UUID.randomUUID(), player.getUniqueId(),
                                    config.storageContext, chestKey, finalAmount));
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

        item.editMeta(x -> {
            x.getPersistentDataContainer().set(chestKey, PersistentDataType.STRING, chest.getKey());
        });
        player.getInventory().addItem(item);
    }

    /**
     * Handles the chest placement animation, sound effects, and the rewarding process.
     *
     * @param player The player who placed the chest.
     * @param chest  The chest type being placed.
     * @param item   The item stack used for placement.
     * @param block  The block where the chest was placed.
     */
    public void handlePlaceChest(final Player player, final Chest chest, final ItemStack item, final Block block) {
        // Get the location of the block where the chest is being placed.
        final Location location = block.getLocation();

        chestsUnderUnlocking.add(location);
        playersUnlocking.add(player.getUniqueId());

        // Check if the item is in the player's off-hand and reduce its amount or remove it from inventory.
        var offHand = player.getInventory().getItemInOffHand();
        if (isChestItem(offHand, chest.getKey())) {
            offHand.setAmount(offHand.getAmount() - 1);
            player.getInventory().setItemInOffHand(offHand);
        } else {
            player.getInventory().removeItem(item.asQuantity(1));
        }

        //#region Animate

        // Temporarily set the block to the chest material and play initial particle effects.
        setTemporaryBlock(player, chest.getBlockMaterial(), block);
        this.playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 10, 0.3, 0.3, 0.3, 0.1);

        // Create a new task to handle the animation phases.
        plugin.scheduler().runRepeating(new YggraTask() {
            int phase = 0;

            @Override
            public void run() {
                switch (this.phase) {
                    case 0: {
                        // Play the chest opening sound and particle effects.
                        playSound(location, chest.getOpenSound());
                        playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 20, 0.5, 0.5, 0.5, 0.15);
                        plugin.scheduler().runLater(new YggraTask() {
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
                        playParticleEffect(location.clone().add(0.5, 0.8, 0.5), chest.getParticle(), 15, 0.3, 0.3, 0.3, 0.1);
                        break;
                    }
                    case 2: {
                        // Play more particle effects and a firework effect if the chest is high-tier.
                        playParticleEffect(location.clone().add(0.5, 1.2, 0.5), chest.getParticle(), 25, 0.6, 0.6, 0.6, 0.2);
                        if (!chest.isHighTier()) break;
                        playParticleEffect(location.clone().add(0.5, 1.5, 0.5), "FIREWORK", 10, 0.8, 0.8, 0.8, 0.3);
                        break;
                    }
                    case 3: {
                        playSound(location, chest.getCloseSound());
                        plugin.scheduler().runLater(new YggraTask() {
                            @Override
                            public void run() {
                                playChestAnimation(block, false);
                            }
                        }, 1L, location);
                    }
                    case 4: {
                        this.cancel();

                        plugin.scheduler().runLater(new YggraTask() {
                            @Override
                            public void run() {
                                playersUnlocking.remove(player.getUniqueId());
                                playCompletionEffects(location, chest);
                                prizeManager.reward(player, chest);
                            }
                        }, 10L, location);
                    }
                }
                ++this.phase;
            }
        }, 10L, 15L, location);

        //#endregion
    }

    /**
     * Plays a sound at the given location.
     *
     * @param location The location to play the sound.
     * @param soundKey The sound key to play.
     */
    private void playSound(Location location, String soundKey) {
        var result = plugin.sound().get(soundKey);
        result.ifPresent(sound -> {
            plugin.sound().playAt(location, sound);
        });
    }

    /**
     * Spawns a particle effect at the given location.
     *
     * @param location     The center location.
     * @param particleType The particle type name.
     * @param count        The number of particles.
     * @param offsetX      The X offset.
     * @param offsetY      The Y offset.
     * @param offsetZ      The Z offset.
     * @param speed        The particle speed.
     */
    private void playParticleEffect(Location location, String particleType, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            Particle particle = this.getParticleSafely(particleType);
            location.getWorld().spawnParticle(Objects.requireNonNullElse(particle, Particle.ENCHANT), location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Exception ex) {
            logger.error("Error playing particle effect.", ex);
        }
    }

    /**
     * Resolves a particle by its namespaced key, returning null if not found.
     *
     * @param particleType The particle type name.
     * @return The particle, or null if not resolvable.
     */
    private Particle getParticleSafely(String particleType) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(particleType.toLowerCase());
            return Registry.PARTICLE_TYPE.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Plays the completion effects (particles, sounds, lightning) after a chest is unlocked.
     *
     * @param location The chest location.
     * @param chest    The chest type.
     */
    private void playCompletionEffects(final Location location, final Chest chest) {
        this.playParticleEffect(location.clone().add(0.5, 0.5, 0.5), chest.getParticle(), chest.getParticleCount(), 0.8, 0.8, 0.8, 0.3);

        plugin.scheduler().runLater(new YggraTask() {
            @Override
            public void run() {
                playSound(location, chest.getCompletionSound());
                playParticleEffect(location.clone().add(0.5, 1.5, 0.5), "FIREWORK", 10, 0.2, 0.2, 0.2, 0.1);
                if (chest.isHighTier()) {
                    playParticleEffect(location.clone().add(0.5, 2.0, 0.5), "END_ROD", 15, 1.0, 1.0, 1.0, 0.5);
                }
                if (chest.isSpawnLightningStrike()) {
                    location.getWorld().strikeLightningEffect(location);
                }
            }
        }, 1L, location);
    }

    /**
     * Temporarily sets a block to the given material and removes it after a delay.
     *
     * @param player   The player for block rotation.
     * @param material The material to set.
     * @param block    The target block.
     */
    private void setTemporaryBlock(final Player player, final Material material, final Block block) {
        plugin.scheduler().run(new YggraTask() {
            @Override
            public void run() {
                block.setType(material);
                block.getState().update(true);
                rotateChestToPlayer(block, player);
            }
        }, block.getLocation());


        plugin.scheduler().runLater(new YggraTask() {
            @Override
            public void run() {
                block.setType(Material.AIR);
                chestsUnderUnlocking.remove(block.getLocation());
            }
        }, 70L, block.getLocation());
    }

    /**
     * Rotates a directional block to face toward the player.
     *
     * @param block  The block to rotate.
     * @param player The player to face.
     */
    private void rotateChestToPlayer(Block block, Player player) {
        if (!(block.getBlockData() instanceof Directional directionalData))
            return;
        Location playerLoc = player.getLocation();
        Location blockLoc = block.getLocation();
        var direction = playerLoc.toVector().subtract(blockLoc.toVector()).normalize();
        BlockFace face = getDirectionFromVector(direction);
        directionalData.setFacing(face);
        block.setBlockData(directionalData);
    }

    /**
     * Converts a direction vector to the nearest cardinal BlockFace.
     *
     * @param direction The direction vector.
     * @return The nearest cardinal BlockFace.
     */
    private BlockFace getDirectionFromVector(org.bukkit.util.Vector direction) {
        double x = direction.getX();
        double z = direction.getZ();

        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    /**
     * Sends a block animation packet to nearby players to play the chest open/close animation.
     *
     * @param chestBlock The chest block.
     * @param open       True to open, false to close.
     */
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

            for (var player : chestBlock.getLocation().getNearbyPlayers(8.0, 5.0, 8.0))
                plugin.protocols().sendServerPacket(player, packet);

        } catch (Exception ex) {
            logger.error("Failed to play chest animation.", ex);
        }
    }

    /**
     * Checks whether an item stack is a chest item with the given key.
     *
     * @param item     The item stack to check.
     * @param chestKey The expected chest key.
     * @return True if the item matches the chest key.
     */
    private boolean isChestItem(ItemStack item, String chestKey) {
        if (!item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return chestKey.equals(pdc.get(this.chestKey, PersistentDataType.STRING));
    }
}
