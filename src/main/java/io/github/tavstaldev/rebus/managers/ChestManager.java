package io.github.tavstaldev.rebus.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.cryptomorin.xseries.XSound;
import io.github.tavstaldev.minecorelib.utils.TypeUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.models.RebusChest;
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

public class ChestManager {
    private final String[] chestResources = new String[] { "daily", "default", "pandora", "choosen" };
    private final NamespacedKey _chestKey = new NamespacedKey(Rebus.Instance, "rebus_chest");
    public NamespacedKey getChestKey() {
        return _chestKey;
    }
    private Set<RebusChest> chests;
    public Set<RebusChest> getChests() {
        return chests;
    }
    private final HashMap<Integer, ItemStack> itemTable = new HashMap<>();
    public HashMap<Integer, ItemStack> getItemTable() {
        return itemTable;
    }

    public final Set<UUID> playersUnlocking = new HashSet<>();
    public final Set<Location> chestsUnderUnlocking = new HashSet<>();

    public void load() {
        chests = (chests == null) ? new HashSet<>() : new HashSet<>(chests);

        // Ensure chests directory exists and create default chest files if necessary
        var chestsDir = Paths.get(Rebus.Instance.getDataFolder().getPath(), "chests").toFile();
        if (!chestsDir.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                chestsDir.mkdirs();
                for (var resource : chestResources) {
                    var filePath = Paths.get(chestsDir.getPath(), resource + ".yml");
                    try (InputStream inputStream = Rebus.Instance.getResource("chests/" + resource + ".yml")) {
                        if (inputStream == null) {
                            Rebus.Logger().Debug(String.format("Failed to get resource file for chest '%s'.", resource));
                        } else {
                            Files.copy(inputStream, filePath);
                        }
                    } catch (IOException ex) {
                        Rebus.Logger().Warn(String.format("Failed to create file for chest '%s'.", resource));
                        Rebus.Logger().Error(ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                Rebus.Logger().Error("Failed to create chests directory: " + ex.getMessage());
                return;
            }
        }

        // Copy items .yml from resources if it doesn't exist
        File itemsFile = Paths.get(Rebus.Instance.getDataFolder().getPath(), "items.yml").toFile();
        if (!itemsFile.exists()) {
            try (InputStream inputStream = Rebus.Instance.getResource("items.yml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, itemsFile.toPath());
                } else {
                    Rebus.Logger().Warn("Failed to get resource file for items.yml.");
                }
            } catch (IOException ex) {
                Rebus.Logger().Warn("Failed to create items.yml file.");
                Rebus.Logger().Error(ex.getMessage());
            }
        }

        // Load items from items.yml
        try (FileInputStream stream = new FileInputStream(itemsFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(stream);

            if (yamlMap == null || !yamlMap.containsKey("items")) {
                Rebus.Logger().Warn("Invalid items data.");
                return;
            }

            List<Map<String, Object>> itemsList = TypeUtils.castAsListOfMaps(yamlMap.get("items"), Rebus.Logger());
            if (itemsList == null) {
                Rebus.Logger().Warn("Invalid items section in items.yml.");
                return;
            }

            for (Map<String, Object> itemData : itemsList) {
                int id = itemData.containsKey("id") ? ((Number) itemData.get("id")).intValue() : -1;
                var itemStack = Rebus.ItemSerializer().deserializeItemStack(itemData);
                itemTable.put(id, itemStack);
            }

        }
        catch (Exception ex) {
            Rebus.Logger().Warn("Error loading items.yml: " + ex.getMessage());
            return;
        }


        // Load chests from configuration
        var chestsSection = Rebus.Config().getConfigurationSection("chests");
        if (chestsSection == null) {
            Rebus.Logger().Warn("No chests section found in configuration.");
            chests.clear();
            return;
        }

        for (String key : chestsSection.getKeys(false)) {
            if (key == null || key.isEmpty()) {
                Rebus.Logger().Warn("Found chest with empty key in configuration.");
                continue;
            }

            var chestSection = chestsSection.getConfigurationSection(key);
            if (chestSection == null) {
                Rebus.Logger().Warn("Invalid chest configuration for key: " + key);
                continue;
            }

            var chest = RebusChest.fromMap(key, chestSection);
            if (chest != null) {
                chests.add(chest);
            }
        }
    }

    public @Nullable RebusChest getByKey(String key) {
        for (var chest : chests) {
            if (chest.getKey().equalsIgnoreCase(key)) {
                return chest;
            }
        }
        return null;
    }

    public void handlePlaceChest(final Player player, final RebusChest chest, final ItemStack item, final Block block) {
        final Location location = block.getLocation();
        chestsUnderUnlocking.add(location);
        playersUnlocking.add(player.getUniqueId());
        if (item.getAmount() <= 1) {
            player.getInventory().remove(item);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), item);
        }

        //#region Animate

        setTemporaryBlock(player, chest.getMaterial(), block);
        this.playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 10, 0.3, 0.3, 0.3, 0.1);
        new BukkitRunnable() {
            int phase = 0;

            public void run() {
                switch (this.phase) {
                    case 0: {
                        XSound sound = XSound.of(chest.getOpenSound()).orElse(XSound.ENTITY_PIGLIN_ANGRY);
                        sound.play(location, 1.0f, 1.0f);
                        playParticleEffect(location.clone().add(0.5, 1.0, 0.5), chest.getParticle(), 20, 0.5, 0.5, 0.5, 0.15);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                playChestAnimation(player, block, true);
                            }
                        }.runTaskLater(Rebus.Instance, 1L);
                        break;
                    }
                    case 1: {
                        playParticleEffect(location.clone().add(0.5, 0.8, 0.5),  chest.getParticle(), 15, 0.3, 0.3, 0.3, 0.1);
                        break;
                    }
                    case 2: {
                        playParticleEffect(location.clone().add(0.5, 1.2, 0.5),  chest.getParticle(), 25, 0.6, 0.6, 0.6, 0.2);
                        if (!chest.isHighTier()) break;
                        playParticleEffect(location.clone().add(0.5, 1.5, 0.5), "FIREWORK", 10, 0.8, 0.8, 0.8, 0.3);
                        break;
                    }
                    case 3: {
                        XSound sound = XSound.of(chest.getCloseSound()).orElse(XSound.ENTITY_PIGLIN_ANGRY);
                        sound.play(location, 0.8f, 1.1f);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                playChestAnimation(player, block, false);
                            }
                        }.runTaskLater(Rebus.Instance, 1L);
                    }
                }
                ++this.phase;
                if (this.phase >= 4) {
                    this.cancel();
                    new BukkitRunnable() {

                        public void run() {
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

    private void playParticleEffect(Location location, String particleType, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            Particle particle = this.getParticleSafely(particleType);
            location.getWorld().spawnParticle(Objects.requireNonNullElse(particle, Particle.ENCHANT), location, count, offsetX, offsetY, offsetZ, speed);
        }
        catch (Exception exception) {
            Rebus.Logger().Error("Error playing particle effect: " + exception.getMessage());
        }
    }

    private Particle getParticleSafely(String particleType) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(particleType.toLowerCase());
            return Registry.PARTICLE_TYPE.get(key);
        }
        catch (Exception e) {
            return null;
        }
    }

    private void playCompletionEffects(Player player, final Location location, final RebusChest chest) {
        float volume = 1.0f;
        float pitch = 1.0f;
        this.playParticleEffect(location.clone().add(0.5, 0.5, 0.5), chest.getParticle(), chest.getParticleCount(), 0.8, 0.8, 0.8, 0.3);
        XSound completionSound = XSound.of(chest.getCompletionSound()).orElse(XSound.ENTITY_PIGLIN_ANGRY);
        completionSound.play(player, volume, pitch);
        new BukkitRunnable(){

            public void run() {
                playParticleEffect(location.clone().add(0.5, 1.5, 0.5), "FIREWORK", 10, 0.2, 0.2, 0.2, 0.1);
                if (chest.isHighTier()) {
                    playParticleEffect(location.clone().add(0.5, 2.0, 0.5), "END_ROD", 15, 1.0, 1.0, 1.0, 0.5);
                }
            }
        }.runTaskLater(Rebus.Instance, 5L);
    }

    private void setTemporaryBlock(final Player player, final Material material, final Block block) {
        new BukkitRunnable(){
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

    private BlockFace getDirectionFromVector(org.bukkit.util.Vector direction) {
        double x = direction.getX();
        double z = direction.getZ();

        if (Math.abs(x) > Math.abs(z)) {
            if (x > 0) {
                return BlockFace.EAST;
            } else {
                return BlockFace.WEST;
            }
        } else {
            if (z > 0) {
                return BlockFace.SOUTH;
            } else {
                return BlockFace.NORTH;
            }
        }
    }

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

            Rebus.Protocols().sendServerPacket(player, packet);

        } catch (Exception e) {
            Rebus.Logger().Error("Failed to play chest animation: " + e.getMessage());
        }
    }
}
