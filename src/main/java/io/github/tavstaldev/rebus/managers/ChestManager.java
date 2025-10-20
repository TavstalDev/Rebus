package io.github.tavstaldev.rebus.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import io.github.tavstaldev.minecorelib.utils.TypeUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.models.RebusChest;
import io.github.tavstaldev.rebus.util.SoundUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages the chests in the Rebus plugin, including loading, handling placement, and animations.
 */
public class ChestManager {
    // Array of default chest resource names.
    private final String[] chestResources = new String[] { "daily", "default", "pandora", "choosen" };

    // Namespaced key used to identify chests in the plugin.
    private final NamespacedKey _chestKey = new NamespacedKey(Rebus.Instance, "rebus_chest");

    /**
     * Retrieves the namespaced key for chests.
     *
     * @return The namespaced key for chests.
     */
    public NamespacedKey getChestKey() {
        return _chestKey;
    }

    // Set of loaded chests.
    private Set<RebusChest> chests;

    /**
     * Retrieves the set of loaded chests.
     *
     * @return A set of RebusChest objects.
     */
    public Set<RebusChest> getChests() {
        return chests;
    }

    // Map of item IDs to ItemStack objects.
    private final HashMap<Integer, ItemStack> itemTable = new HashMap<>();

    /**
     * Retrieves the item table mapping item IDs to ItemStacks.
     *
     * @return A HashMap of item IDs to ItemStack objects.
     */
    public HashMap<Integer, ItemStack> getItemTable() {
        return itemTable;
    }

    // Set of players currently unlocking chests.
    public final Set<UUID> playersUnlocking = new HashSet<>();

    // Set of chest locations currently under unlocking.
    public final Set<Location> chestsUnderUnlocking = new HashSet<>();

    /**
     * Loads chests and items from configuration and resource files.
     * Ensures necessary directories and files exist, and initializes the chest and item data.
     */
    public void load() {
        // Initialize the set of chests.
        chests = new HashSet<>();

        // Ensure the chests directory exists and create default chest files if necessary.
        var chestsDir = Paths.get(Rebus.Instance.getDataFolder().getPath(), "chests").toFile();
        if (!chestsDir.exists()) {
            try {
                // Create the chests directory.
                //noinspection ResultOfMethodCallIgnored
                chestsDir.mkdirs();
                for (var resource : chestResources) {
                    var filePath = Paths.get(chestsDir.getPath(), resource + ".yml");
                    try (InputStream inputStream = Rebus.Instance.getResource("chests/" + resource + ".yml")) {
                        if (inputStream == null) {
                            // Log a debug message if the resource file is not found.
                            Rebus.logger().debug(String.format("Failed to get resource file for chest '%s'.", resource));
                        } else {
                            // Copy the resource file to the chests directory.
                            Files.copy(inputStream, filePath);
                        }
                    } catch (IOException ex) {
                        // Log a warning and error message if file creation fails.
                        Rebus.logger().warn(String.format("Failed to create file for chest '%s'.", resource));
                        Rebus.logger().error(ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                // Log an error message if the chests directory creation fails.
                Rebus.logger().error("Failed to create chests directory: " + ex.getMessage());
                return;
            }
        }

        // Copy the items.yml file from resources if it doesn't exist.
        File itemsFile = Paths.get(Rebus.Instance.getDataFolder().getPath(), "items.yml").toFile();
        if (!itemsFile.exists()) {
            try (InputStream inputStream = Rebus.Instance.getResource("items.yml")) {
                if (inputStream != null) {
                    // Copy the items.yml file to the data folder.
                    Files.copy(inputStream, itemsFile.toPath());
                } else {
                    // Log a warning if the resource file is not found.
                    Rebus.logger().warn("Failed to get resource file for items.yml.");
                }
            } catch (IOException ex) {
                // Log a warning and error message if file creation fails.
                Rebus.logger().warn("Failed to create items.yml file.");
                Rebus.logger().error(ex.getMessage());
            }
        }

        // Load items from the items.yml file.
        try (FileInputStream stream = new FileInputStream(itemsFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(stream);

            // Validate the items data.
            if (yamlMap == null || !yamlMap.containsKey("items")) {
                Rebus.logger().warn("Invalid items data.");
                return;
            }

            // Parse the items section and populate the item table.
            List<Map<String, Object>> itemsList = TypeUtils.castAsListOfMaps(yamlMap.get("items"), Rebus.logger());
            if (itemsList == null) {
                Rebus.logger().warn("Invalid items section in items.yml.");
                return;
            }

            for (Map<String, Object> itemData : itemsList) {
                // Retrieve the item ID and deserialize the item stack.
                int id = itemData.containsKey("id") ? ((Number) itemData.get("id")).intValue() : -1;
                var itemStack = Rebus.itemSerializer().deserializeItemStack(itemData);
                itemTable.put(id, itemStack);
            }

        } catch (Exception ex) {
            // Log a warning if an error occurs while loading items.yml.
            Rebus.logger().warn("Error loading items.yml: " + ex.getMessage());
            return;
        }

        // Load chests from the configuration.
        var chestsSection = Rebus.config().getConfigurationSection("chests");
        if (chestsSection == null) {
            // Log a warning if the chests section is not found in the configuration.
            Rebus.logger().warn("No chests section found in configuration.");
            return;
        }

        for (String key : chestsSection.getKeys(false)) {
            if (key == null || key.isEmpty()) {
                // Log a warning if a chest with an empty key is found.
                Rebus.logger().warn("Found chest with empty key in configuration.");
                continue;
            }

            var chestSection = chestsSection.getConfigurationSection(key);
            if (chestSection == null) {
                // Log a warning if the chest configuration is invalid.
                Rebus.logger().warn("Invalid chest configuration for key: " + key);
                continue;
            }

            // Create a RebusChest instance from the configuration and add it to the set.
            var chest = RebusChest.fromMap(key, chestSection);
            if (chest != null) {
                chests.add(chest);
            }
        }
    }

    /**
     * Retrieves a chest by its unique key.
     *
     * @param key The unique key of the chest.
     * @return The RebusChest object if found, or null if not found.
     */
    public @Nullable RebusChest getByKey(String key) {
        for (var chest : chests) {
            if (chest.getKey().equalsIgnoreCase(key)) {
                return chest;
            }
        }
        return null;
    }

    /**
     * Handles the placement of a chest by a player, including animations and effects.
     *
     * @param player The player placing the chest.
     * @param chest  The RebusChest being placed.
     * @param item   The ItemStack representing the chest.
     * @param block  The block where the chest is being placed.
     */
    public void handlePlaceChest(final Player player, final RebusChest chest, final ItemStack item, final Block block) {
        // Get the location of the block where the chest is being placed.
        final Location location = block.getLocation();

        // Add the location to the set of chests under unlocking.
        chestsUnderUnlocking.add(location);

        // Add the player to the set of players currently unlocking chests.
        playersUnlocking.add(player.getUniqueId());

        // Check if the item is in the player's off-hand and reduce its amount or remove it from inventory.
        var offHand = player.getInventory().getItemInOffHand();
        if (offHand.isSimilar(item.asOne()) && offHand.getAmount() >= 1) {
            offHand.setAmount(offHand.getAmount() - 1);
            player.getInventory().setItemInOffHand(offHand);
        } else {
            player.getInventory().removeItem(item.asQuantity(1));
        }

        //#region Animate

        // Temporarily set the block to the chest material and play initial particle effects.
        setTemporaryBlock(player, chest.getMaterial(), block);
        this.playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 10, 0.3, 0.3, 0.3, 0.1);

        // Create a new task to handle the animation phases.
        new BukkitRunnable() {
            int phase = 0;

            public void run() {
                switch (this.phase) {
                    case 0: {
                        // Play the chest opening sound and particle effects.
                        var sound = SoundUtils.getSound(chest.getOpenSound());
                        if (sound.isEmpty()) {
                            Rebus.logger().warn("Invalid sound: " + chest.getOpenSound());
                        }
                        else {
                            location.getWorld().playSound(sound.get(), location.getX(), location.y(), location.z());
                        }
                        playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 20, 0.5, 0.5, 0.5, 0.15);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // Play the chest opening animation.
                                playChestAnimation(player, block, true);
                            }
                        }.runTaskLater(Rebus.Instance, 1L);
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
                        // Play the chest closing sound and animation.
                        var sound = SoundUtils.getSound(chest.getCloseSound());
                        if (sound.isEmpty()) {
                            Rebus.logger().warn("Invalid sound: " + chest.getOpenSound());
                        }
                        else {
                            location.getWorld().playSound(sound.get(), location.getX(), location.y(), location.z());
                        }
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                playChestAnimation(player, block, false);
                            }
                        }.runTaskLater(Rebus.Instance, 1L);
                    }
                }
                ++this.phase;

                // End the animation after all phases are complete.
                if (this.phase >= 4) {
                    this.cancel();
                    new BukkitRunnable() {
                        public void run() {
                            // Remove the location and player from the unlocking sets and play completion effects.
                            chestsUnderUnlocking.remove(location);
                            playersUnlocking.remove(player.getUniqueId());
                            playCompletionEffects(player, location, chest);
                            chest.reward(player);
                        }
                    }.runTaskLater(Rebus.Instance, 10L);
                }
            }
        }.runTaskTimer(Rebus.Instance, 10L, 15L);

        //#endregion
    }


    /**
     * Plays a particle effect at the specified location.
     *
     * @param location   The location where the particle effect will be displayed.
     * @param particleType The type of particle to display.
     * @param count      The number of particles to spawn.
     * @param offsetX    The offset on the X-axis for particle spread.
     * @param offsetY    The offset on the Y-axis for particle spread.
     * @param offsetZ    The offset on the Z-axis for particle spread.
     * @param speed      The speed of the particle effect.
     */
    private void playParticleEffect(Location location, String particleType, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            Particle particle = this.getParticleSafely(particleType);
            location.getWorld().spawnParticle(Objects.requireNonNullElse(particle, Particle.ENCHANT), location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Exception exception) {
            Rebus.logger().error("Error playing particle effect: " + exception.getMessage());
        }
    }

    /**
     * Safely retrieves a Particle object from its string representation.
     *
     * @param particleType The string representation of the particle type.
     * @return The corresponding Particle object, or null if not found.
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
     * Plays completion effects for a chest, including particles and sounds.
     *
     * @param player   The player for whom the effects are played.
     * @param location The location of the chest.
     * @param chest    The RebusChest object representing the chest.
     */
    private void playCompletionEffects(Player player, final Location location, final RebusChest chest) {
        this.playParticleEffect(location.clone().add(0.5, 0.5, 0.5), chest.getParticle(), chest.getParticleCount(), 0.8, 0.8, 0.8, 0.3);
        var sound = SoundUtils.getSound(chest.getCompletionSound());
        if (sound.isEmpty()) {
            Rebus.logger().warn("Invalid sound: " + chest.getOpenSound());
        }
        else {
            location.getWorld().playSound(sound.get(), location.getX(), location.y(), location.z());
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                playParticleEffect(location.clone().add(0.5, 1.5, 0.5), "FIREWORK", 10, 0.2, 0.2, 0.2, 0.1);
                if (chest.isHighTier()) {
                    playParticleEffect(location.clone().add(0.5, 2.0, 0.5), "END_ROD", 15, 1.0, 1.0, 1.0, 0.5);
                }
            }
        }.runTaskLater(Rebus.Instance, 5L);
    }

    /**
     * Temporarily sets a block to a specific material and reverts it after a delay.
     *
     * @param player   The player interacting with the block.
     * @param material The material to temporarily set the block to.
     * @param block    The block to be modified.
     */
    private void setTemporaryBlock(final Player player, final Material material, final Block block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(material);
                block.getState().update(true);
                rotateChestToPlayer(block, player);
            }
        }.runTask(Rebus.Instance);

        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(Material.AIR);
            }
        }.runTaskLater(Rebus.Instance, 65L);
    }

    /**
     * Rotates a chest block to face the player.
     *
     * @param block  The chest block to rotate.
     * @param player The player to face the chest towards.
     */
    private void rotateChestToPlayer(Block block, Player player) {
        if (!(block.getBlockData() instanceof Directional directionalData))
            return;
        Location playerLoc = player.getLocation();
        Location blockLoc = block.getLocation();
        var direction = playerLoc.toVector().subtract(blockLoc.toVector());
        BlockFace face = getDirectionFromVector(direction);
        directionalData.setFacing(face);
        block.setBlockData(directionalData);
    }

    /**
     * Determines the BlockFace direction from a vector.
     *
     * @param direction The vector representing the direction.
     * @return The corresponding BlockFace.
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
     * Plays a chest animation (open or close) for a player.
     *
     * @param player     The player for whom the animation is played.
     * @param chestBlock The chest block to animate.
     * @param open       True to play the open animation, false for close.
     */
    public static void playChestAnimation(Player player, Block chestBlock, boolean open) {
        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_ACTION);

            Location loc = chestBlock.getLocation();
            packet.getBlockPositionModifier().write(0, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            packet.getBlocks().write(0, chestBlock.getType());

            // The action ID: 1 for chests
            packet.getIntegers().write(0, 1);

            // The action parameter: 1 for open, 0 for close
            packet.getIntegers().write(1, open ? 1 : 0);

            Rebus.protocols().sendServerPacket(player, packet);

        } catch (Exception e) {
            Rebus.logger().error("Failed to play chest animation: " + e.getMessage());
        }
    }
}
