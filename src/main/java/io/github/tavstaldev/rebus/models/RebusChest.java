package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.minecorelib.utils.TypeUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.util.IconUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.*;

public class RebusChest {
    private final String key;
    private final String name;
    private final int rolls;
    private final List<String> description;
    private final Material material;
    private final double cost;
    private final long cooldown;
    private final long buyCooldown;
    private final String permission;
    private final int slot;
    private final String particle;
    private final int particleCount;
    private final String openSound;
    private final String closeSound;
    private final String completionSound;
    private final boolean isHighTier;
    private final Set<Reward> rewards;
    private final Set<ItemStack> itemCache = new HashSet<>();

    public RebusChest(String key, String name, int rolls, List<String> description, Material material, double cost, long cooldown, long buyCooldown, String permission, int slot, String particle, int particleCount, String openSound, String closeSound, String completionSound, boolean isHighTier, Set<Reward> rewards) {
        this.key = key;
        this.name = name;
        this.rolls = rolls;
        this.description = description;
        this.material = material;
        this.cost = cost;
        this.cooldown = cooldown;
        this.buyCooldown = buyCooldown;
        this.permission = permission;
        this.slot = slot;
        this.particle = particle;
        this.particleCount = particleCount;
        this.openSound = openSound;
        this.closeSound = closeSound;
        this.completionSound = completionSound;
        this.isHighTier = isHighTier;
        this.rewards = rewards;
    }

    public void give(Player player, int amount) {
        ItemStack item = new ItemStack(material, amount);
        var itemMeta = item.getItemMeta();
        itemMeta.displayName(ChatUtils.translateColors(getName(), true));
        List<Component> lore = new ArrayList<>();
        for (String line : getDescription()) {
            lore.add(ChatUtils.translateColors(line, true));
        }
        itemMeta.lore(lore);
        itemMeta.getPersistentDataContainer().set(Rebus.chestManager().getChestKey(), PersistentDataType.STRING, key);
        item.setItemMeta(itemMeta);
        player.getInventory().addItem(item);
    }

    public void reward(Player player) {
        if (rewards.isEmpty()) {
            //Rebus.Instance.sendLocalizedMsg(player, "Chests.NoRewards");
            return;
        }

        int totalChance = rewards.stream().mapToInt(Reward::getChance).sum();
        ArrayList<ItemStack> items = new ArrayList<>();
        boolean inventoryFullMessageSent = false;
        var location = player.getLocation();
        for(int i = 0; i < rolls; ) {
            ItemStack item = rollRewards(totalChance);
            while(items.contains(item)) {
                item = rollRewards(totalChance);
            }
            items.add(item);

            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                if (!inventoryFullMessageSent) {
                    Rebus.Instance.sendLocalizedMsg(player, "Chests.InventoryFull");
                }
                location.getWorld().dropItemNaturally(location, item);
            }

            i++;
        }
        items.clear();

        Rebus.database().addCooldown(player.getUniqueId(), ECooldownType.OPEN, key, cooldown);
        Rebus.Instance.sendLocalizedMsg(player, "Chests.RewardReceived", Map.of("chest_name", getName()));

        //Rebus.Instance.sendLocalizedMsg(player, "Chests.NoRewards");
    }

    public ItemStack rollRewards(int totalChance) {
        int randomValue = (int) (Math.random() * totalChance);
        int cumulativeChance = 0;

        for (Reward reward : rewards) {
            List<ItemStack> test = new ArrayList<>(reward.getItemStacks());
            cumulativeChance += reward.getChance();
            if (randomValue < cumulativeChance) {
                return test.getFirst();
            }
        }
        return null; // unlucky
    }

    public static @Nullable RebusChest fromMap(String key, ConfigurationSection values) {
        String name = values.getString("name");
        List<String> description = values.getStringList("description");
        Material material = IconUtils.getMaterial(values.getString("material"));
        double cost = values.getDouble("cost", 0);
        long cooldown = values.getLong("cooldown", 0);
        long buyCooldown = values.getLong("buyCooldown", 0);
        String permission = values.getString("permission", "rebus.use").isEmpty() ? "rebus.use" : values.getString("permission");
        int slot = values.getInt("slot", 0);

        String particle = values.getString("particle", "ENCHANT");
        int particleCount = values.getInt("particleCount", 30);

        String openSound = values.getString("openSound", "BLOCK_CHEST_OPEN");
        String closeSound = values.getString("closeSound", "BLOCK_CHEST_CLOSE");
        String completionSound = values.getString("completionSound", "ENTITY_PLAYER_LEVELUP");

        boolean isHighTier = values.getBoolean("isHighTier", false);

        Set<Reward> rewards = new HashSet<>();
        File rewardFile = Paths.get(Rebus.Instance.getDataFolder().getPath(), "chests", key + ".yml").toFile();
        if (!rewardFile.exists()) {
            Rebus.logger().warn("Reward file not found for chest: " + key);
            return null;
        }

        Map<String, Object> yamlMap;
        try (FileInputStream stream = new FileInputStream(rewardFile)) {
            Yaml yaml = new Yaml();
            yamlMap = TypeUtils.castAsMap(yaml.load(stream), null);

            if (yamlMap == null || !yamlMap.containsKey("data")) {
                Rebus.logger().warn("Invalid reward data for chest: " + key);
                return null;
            }

            int rolls = (int) yamlMap.getOrDefault("rolls", 1);

            Map<String, Object> dataMap = TypeUtils.castAsMap(yamlMap.get("data"), null);
            if (dataMap == null) {
                Rebus.logger().warn("Invalid data section for chest: " + key);
                return null;
            }

            for (var entry : dataMap.entrySet()) {
                Map<String, Object> dataEntry = TypeUtils.castAsMap(entry.getValue(), null);
                if (dataEntry == null) continue;

                int chance = dataEntry.containsKey("chance") ? ((Number) dataEntry.get("chance")).intValue() : 25;
                List<Integer> itemsRaw = TypeUtils.castAsList(dataEntry.get("items"), Rebus.logger());
                if (itemsRaw == null) return null;
                Set<Integer> items = new HashSet<>(itemsRaw);

                rewards.add(new Reward(chance, items));
            }
            return new RebusChest(key, name, rolls,
                    description, material, cost, cooldown, buyCooldown, permission, slot, particle, particleCount,
                    openSound, closeSound, completionSound, isHighTier, rewards);
        } catch (Exception ex) {
            Rebus.logger().warn("Error loading reward file for chest: " + key);
            Rebus.logger().warn(ex.getMessage());
            return null;
        }
    }

    //#region Getters
    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public Material getMaterial() {
        return material;
    }

    public double getCost() {
        return cost;
    }

    public long getCooldown() {
        return cooldown;
    }

    public long getBuyCooldown() {
        return buyCooldown;
    }

    public String getPermission() {
        return permission;
    }

    public int getSlot() {
        return slot;
    }

    public Set<Reward> getRewards() {
        return rewards;
    }

    public Set<ItemStack> getPossibleItems() {
        if (!itemCache.isEmpty())
            return itemCache;

        Set<ItemStack> items = new HashSet<>();
        for (Reward reward : rewards) {
            items.addAll(reward.getItemStacks());
        }
        itemCache.addAll(items);
        return items;
    }

    public String getParticle() {
        return particle;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public String getOpenSound() {
        return openSound;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public String getCompletionSound() {
        return completionSound;
    }

    public boolean isHighTier() {
        return isHighTier;
    }
    //#endregion
}
