package io.github.tavstaldev.rebus.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.cryptomorin.xseries.XSound;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.models.RebusChest;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ChestManager {
    private final String[] chestResources = new String[] { "daily", "bronze", "silver", "gold", "diamond", "netherite", "mythic" };
    private final NamespacedKey _chestKey = new NamespacedKey(Rebus.Instance, "rebus_chest");
    public NamespacedKey getChestKey() {
        return _chestKey;
    }
    private Set<RebusChest> chests;
    public Set<RebusChest> getChests() {
        return chests;
    }

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
        if (item.getAmount() <= 1) {
            player.getInventory().remove(item);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), item);
        }

        //#region Animate
        setTemporaryBlock(player, chest.getMaterial(), block, 65L);
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
            if (particle != null) {
                location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            } else {
                location.getWorld().spawnParticle(Particle.ENCHANT, location, count, offsetX, offsetY, offsetZ, speed);
            }
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

    private void setTemporaryBlock(final Player player, final Material material, final Block block, final long ticks) {
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
        }.runTaskLater(Rebus.Instance, ticks);
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
