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

/**
 * Represents a RebusChest, which is a custom chest with rewards and various properties.
 */
public class RebusChest {
    // Unique key identifying the chest
    private final String key;

    // Display name of the chest
    private final String name;

    // Description of the chest
    private final List<String> description;

    // Material type of the chest
    private final Material material;

    // Cost to open the chest
    private final double cost;

    // Cooldown time in seconds for the chest
    private final long cooldown;

    private final long buyCooldown;

    // Permission required to use the chest
    private final String permission;

    // Slot position of the chest in a GUI
    private final int slot;

    // Particle effect associated with the chest
    private final String particle;

    // Number of particles to display
    private final int particleCount;

    // Sound played when the chest is opened
    private final String openSound;

    // Sound played when the chest is closed
    private final String closeSound;

    // Sound played when the chest is completed
    private final String completionSound;

    // Indicates if the chest is a high-tier chest
    private final boolean isHighTier;

    // Set of rewards associated with the chest
    private final Set<Reward> rewards;

    private final Set<ItemStack> itemCache = new HashSet<>();

    /**
     * Constructs a RebusChest instance with the specified properties.
     *
     * @param key             Unique key identifying the chest.
     * @param name            Display name of the chest.
     * @param description     Description of the chest.
     * @param material        Material type of the chest.
     * @param cost            Cost to open the chest.
     * @param cooldown        Cooldown time in seconds for the chest.
     * @param permission      Permission required to use the chest.
     * @param slot            Slot position of the chest in a GUI.
     * @param particle        Particle effect associated with the chest.
     * @param particleCount   Number of particles to display.
     * @param openSound       Sound played when the chest is opened.
     * @param closeSound      Sound played when the chest is closed.
     * @param completionSound Sound played when the chest is completed.
     * @param isHighTier      Indicates if the chest is a high-tier chest.
     * @param rewards         Set of rewards associated with the chest.
     */
    public RebusChest(String key, String name, List<String> description, Material material, double cost, long cooldown, long buyCooldown, String permission, int slot, String particle, int particleCount, String openSound, String closeSound, String completionSound, boolean isHighTier, Set<Reward> rewards) {
        this.key = key;
        this.name = name;
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

    /**
     * Gives the chest item to a player.
     *
     * @param player The player to give the chest item to.
     * @param amount The number of chest items to give.
     */
    public void give(Player player, int amount) {
        ItemStack item = new ItemStack(material, amount);
        var itemMeta = item.getItemMeta();
        itemMeta.displayName(ChatUtils.translateColors(getName(), true));
        List<Component> lore = new ArrayList<>();
        for (String line : getDescription()) {
            lore.add(ChatUtils.translateColors(line, true));
        }
        itemMeta.lore(lore);
        // Add a custom persistent data key to the item's metadata to identify the chest.
        itemMeta.getPersistentDataContainer().set(Rebus.chestManager().getChestKey(), PersistentDataType.STRING, key);
        item.setItemMeta(itemMeta);
        player.getInventory().addItem(item);
    }

    /**
     * Rewards a player with items from the chest.
     *
     * @param player The player to reward.
     */
    public void reward(Player player) {
        // Check if there are no rewards available and notify the player.
        if (rewards.isEmpty()) {
            Rebus.Instance.sendLocalizedMsg(player, "Chests.NoRewards");
            return;
        }

        // Calculate the total chance of all rewards.
        int totalChance = rewards.stream().mapToInt(Reward::getChance).sum();
        // Generate a random value within the total chance range.
        int randomValue = (int) (Math.random() * totalChance);
        int cumulativeChance = 0;

        // Iterate through the rewards to determine which reward the player receives.
        for (Reward reward : rewards) {
            cumulativeChance += reward.getChance();
            if (randomValue < cumulativeChance) {
                boolean inventoryFullMessageSent = false;
                var location = player.getLocation();

                // Iterate through the items in the reward.
                for (ItemStack item : reward.getItemStacks()) {
                    ItemStack cloneItem = item.clone();

                    // Handle items with a maximum stack size of 1.
                    if (cloneItem.getMaxStackSize() == 1 && cloneItem.getAmount() > 0) {
                        cloneItem.setAmount(1);
                        for (int i = 0; i < cloneItem.getAmount(); i++) {
                            // Drop the item if the player's inventory is full.
                            if (player.getInventory().firstEmpty() == -1) {
                                location.getWorld().dropItemNaturally(location, cloneItem);
                                if (!inventoryFullMessageSent) {
                                    Rebus.Instance.sendLocalizedMsg(player, "Chests.InventoryFull");
                                    inventoryFullMessageSent = true;
                                }
                                continue;
                            }
                            // Add the item to the player's inventory.
                            player.getInventory().addItem(cloneItem);
                        }
                    } else {
                        var remainder = player.getInventory().addItem(cloneItem);

                        // Check if there are any remaining items.
                        if (!remainder.isEmpty()) {
                            // Drop all remaining items on the ground.
                            for (ItemStack remainingItem : remainder.values()) {
                                location.getWorld().dropItemNaturally(location, remainingItem);
                            }

                            // Send the inventory full message only once.
                            if (!inventoryFullMessageSent) {
                                Rebus.Instance.sendLocalizedMsg(player, "Chests.InventoryFull");
                                inventoryFullMessageSent = true;
                            }
                        }
                    }
                }

                // Add a cooldown for the chest to the player's cache and database.
                Rebus.database().addCooldown(player.getUniqueId(), ECooldownType.OPEN, key, cooldown);

                // Notify the player that they have received a reward.
                Rebus.Instance.sendLocalizedMsg(player, "Chests.RewardReceived", Map.of("chest_name", getName()));
                return;
            }
        }

        // Notify the player if no rewards were received.
        Rebus.Instance.sendLocalizedMsg(player, "Chests.NoRewards");
    }


    /**
     * Creates a RebusChest instance from a configuration map.
     *
     * @param key    The unique key of the chest.
     * @param values The configuration section containing chest properties.
     * @return A RebusChest instance or null if the configuration is invalid.
     */
    public static @Nullable RebusChest fromMap(String key, ConfigurationSection values) {
        //#region Retrieve properties from configuration
        // Retrieve the name of the chest from the configuration.
        String name = values.getString("name");

        // Retrieve the description of the chest as a list of strings.
        List<String> description = values.getStringList("description");

        // Retrieve the material type of the chest.
        Material material = IconUtils.getMaterial(values.getString("material"));

        // Retrieve the cost to open the chest, defaulting to 0 if not specified.
        double cost = values.getDouble("cost", 0);

        // Retrieve the cooldown time in seconds for the chest.
        long cooldown = values.getLong("cooldown", 0);

        long buyCooldown = values.getLong("buyCooldown", 0);

        // Retrieve the permission required to use the chest, defaulting to "rebus.use".
        String permission = values.getString("permission", "rebus.use").isEmpty() ? "rebus.use" : values.getString("permission");

        // Retrieve the slot position of the chest in a GUI.
        int slot = values.getInt("slot", 0);

        // Retrieve the particle effect associated with the chest, defaulting to "ENCHANT".
        String particle = values.getString("particle", "ENCHANT");
        // Retrieve the number of particles to display, defaulting to 30.
        int particleCount = values.getInt("particleCount", 30);

        // Retrieve the sound played when the chest is opened, defaulting to "BLOCK_CHEST_OPEN".
        String openSound = values.getString("openSound", "BLOCK_CHEST_OPEN");
        // Retrieve the sound played when the chest is closed, defaulting to "BLOCK_CHEST_CLOSE".
        String closeSound = values.getString("closeSound", "BLOCK_CHEST_CLOSE");
        // Retrieve the sound played when the chest is completed, defaulting to "ENTITY_PLAYER_LEVELUP".
        String completionSound = values.getString("completionSound", "ENTITY_PLAYER_LEVELUP");

        // Check if the chest is marked as high-tier, defaulting to false.
        boolean isHighTier = values.getBoolean("isHighTier", false);
        //#endregion

        // Initialize a set to hold the rewards associated with the chest.
        Set<Reward> rewards = new HashSet<>();
        // Construct the file path for the reward file based on the chest key.
        File rewardFile = Paths.get(Rebus.Instance.getDataFolder().getPath(), "chests", key + ".yml").toFile();
        // Check if the reward file exists; if not, log a warning and return null.
        if (!rewardFile.exists()) {
            Rebus.logger().warn("Reward file not found for chest: " + key);
            return null;
        }

        // Attempt to load the reward data from the file.
        try (FileInputStream stream = new FileInputStream(rewardFile)) {
            Yaml yaml = new Yaml();
            // Parse the YAML file into a map.
            Map<String, Object> yamlMap = TypeUtils.castAsMap(yaml.load(stream), null);

            // Validate the parsed data and log a warning if invalid.
            if (yamlMap == null || !yamlMap.containsKey("data")) {
                Rebus.logger().warn("Invalid reward data for chest: " + key);
                return null;
            }

            // Retrieve the "data" section of the YAML map.
            Map<String, Object> dataMap = TypeUtils.castAsMap(yamlMap.get("data"), null);
            if (dataMap == null) {
                Rebus.logger().warn("Invalid data section for chest: " + key);
                return null;
            }

            // Iterate through the entries in the data map to construct rewards.
            for (var entry : dataMap.entrySet()) {
                // Cast each entry to a map and validate it.
                Map<String, Object> dataEntry = TypeUtils.castAsMap(entry.getValue(), null);
                if (dataEntry == null) continue;

                // Retrieve the chance of obtaining the reward, defaulting to 25.
                int chance = dataEntry.containsKey("chance") ? ((Number) dataEntry.get("chance")).intValue() : 25;
                // Retrieve the list of item IDs associated with the reward.
                List<Integer> itemsRaw = TypeUtils.castAsList(dataEntry.get("items"), Rebus.logger());
                if (itemsRaw == null) return null;
                // Convert the list of item IDs to a set.
                Set<Integer> items = new HashSet<>(itemsRaw);

                // Add the reward to the set of rewards.
                rewards.add(new Reward(chance, items));
            }
        } catch (Exception ex) {
            // Log any exceptions that occur while loading the reward file.
            Rebus.logger().warn("Error loading reward file for chest: " + key);
            Rebus.logger().warn(ex.getMessage());
            return null;
        }

        // Return a new RebusChest instance with the loaded properties and rewards.
        return new RebusChest(key, name, description, material, cost, cooldown, buyCooldown, permission, slot, particle, particleCount, openSound, closeSound, completionSound, isHighTier, rewards);
    }

    //#region Getters
    /**
     * Gets the unique key of the chest.
     *
     * @return The key of the chest.
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the display name of the chest.
     *
     * @return The name of the chest.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of the chest.
     *
     * @return A list of description lines.
     */
    public List<String> getDescription() {
        return description;
    }

    /**
     * Gets the material type of the chest.
     *
     * @return The material of the chest.
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Gets the cost to open the chest.
     *
     * @return The cost of the chest.
     */
    public double getCost() {
        return cost;
    }

    /**
     * Gets the cooldown time of the chest.
     *
     * @return The cooldown time in seconds.
     */
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Gets the buy cooldown time of the chest.
     *
     * @return The buy cooldown time in seconds.
     */
    public long getBuyCooldown() {
        return buyCooldown;
    }

    /**
     * Gets the permission required to use the chest.
     *
     * @return The permission string.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the slot position of the chest in a GUI.
     *
     * @return The slot position.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Gets the rewards associated with the chest.
     *
     * @return A set of rewards.
     */
    public Set<Reward> getRewards() {
        return rewards;
    }

    /**
     * Retrieves all possible items from the rewards associated with the chest.
     * This method is intended for previewing items in the GUI and should not be used for rewarding players.
     *
     * @return A set of ItemStack objects representing all possible items from the chest's rewards.
     */
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

    /**
     * Gets the particle effect associated with the chest.
     *
     * @return The particle effect name.
     */
    public String getParticle() {
        return particle;
    }

    /**
     * Gets the number of particles to display.
     *
     * @return The particle count.
     */
    public int getParticleCount() {
        return particleCount;
    }

    /**
     * Gets the sound played when the chest is opened.
     *
     * @return The open sound name.
     */
    public String getOpenSound() {
        return openSound;
    }

    /**
     * Gets the sound played when the chest is closed.
     *
     * @return The close sound name.
     */
    public String getCloseSound() {
        return closeSound;
    }

    /**
     * Gets the sound played when the chest is completed.
     *
     * @return The completion sound name.
     */
    public String getCompletionSound() {
        return completionSound;
    }

    /**
     * Checks if the chest is a high-tier chest.
     *
     * @return True if the chest is high-tier, false otherwise.
     */
    public boolean isHighTier() {
        return isHighTier;
    }
    //#endregion
}