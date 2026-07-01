package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.database.models.Cooldown;
import io.github.tavstaldev.rebus.database.models.ECooldownType;
import io.github.tavstaldev.rebus.models.Chest;
import io.github.tavstaldev.rebus.models.EPrizeType;
import io.github.tavstaldev.rebus.models.PrizeData;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import io.github.tavstaldev.yggra.core.scheduler.YggraTask;
import io.github.tavstaldev.yggra.core.services.TranslationService;
import io.github.tavstaldev.yggra.items.api.utils.AdventureUtils;
import io.github.tavstaldev.yggra.items.api.utils.TypeUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages prize definitions loaded from the prizes.yml configuration file.
 */
public class PrizeManager {
    private final Rebus plugin;
    private final RebusConfig config;
    private final YggraLogger logger;
    private final TranslationService translator;
    private final HashMap<String, PrizeData> prizes;
    private final File prizesFile;
    private final Yaml yamlParser;

    /**
     * Creates a new prize manager for the given plugin.
     *
     * @param plugin The plugin instance.
     */
    public PrizeManager(Rebus plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.logger = plugin.logger().withModule(PrizeManager.class);
        this.translator = plugin.translator();
        this.prizes = new HashMap<>();
        prizesFile = Paths.get(plugin.getDataFolder().getPath(), "prizes.yml").toFile();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        this.yamlParser = new Yaml(options);
    }

    /**
     * Loads prizes from the prizes.yml file.
     *
     * @return True if the prizes were loaded successfully, false otherwise.
     */
    public boolean load() {
        prizes.clear();

        // Copy the prizes.yml file from resources if it doesn't exist.
        if (!prizesFile.exists()) {
            plugin.saveResource("prizes.yml", false);
        }

        // Load items from the prizes.yml file.
        try (FileInputStream stream = new FileInputStream(prizesFile)) {
            Map<String, Object> yamlMap = yamlParser.load(stream);

            // Validate the 'data' key.
            if (yamlMap == null || !yamlMap.containsKey("data")) {
                logger.warn("Invalid prizes data.");
                return false;
            }

            // Parse the data section and populate the item table.
            Map<String, Object> dataMap = TypeUtils.castAsMap(yamlMap.get("data"), null);
            if (dataMap == null) {
                logger.warn("Invalid data section in prizes.yml.");
                return false;
            }

            // Check items and commands sections
            Map<String, Object> commandsMap = TypeUtils.castAsMap(dataMap.get("commands"), null);
            if (commandsMap == null) {
                logger.warn("Invalid commands section in prizes.yml.");
                return false;
            }
            Map<String, Object> itemsMap = TypeUtils.castAsMap(dataMap.get("items"), null);
            if (itemsMap == null) {
                logger.warn("Invalid items section in prizes.yml.");
                return false;
            }

            for (var entry : commandsMap.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> commandData = TypeUtils.castAsMap(entry.getValue(), null);
                if (commandData == null) {
                    logger.warn("Invalid command data for key: " + key);
                    continue;
                }

                String command = (String) commandData.get("command");
                if (command == null || command.isEmpty()) {
                    logger.warn("Missing or empty command for key: " + key);
                    continue;
                }

                Map<String, Object> displayItemData = TypeUtils.castAsMap(commandData.get("display"), null);
                if (displayItemData == null || displayItemData.isEmpty()) {
                    logger.warn("Missing or invalid display item for command key: " + key);
                    continue;
                }

                var itemStack = plugin.itemSerializer().deserializeItemStack(displayItemData);
                if (itemStack == null) {
                    logger.warn("Failed to deserialize item for command key: " + key);
                    continue;
                }

                prizes.put(key, new PrizeData(command, itemStack));
            }

            for (var entry : itemsMap.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> itemData = TypeUtils.castAsMap(entry.getValue(), null);
                if (itemData == null) {
                    logger.warn("Invalid item data for key: " + key);
                    continue;
                }

                if (!itemData.containsKey("amount"))
                    itemData.put("amount", 1);

                var itemStack = plugin.itemSerializer().deserializeItemStack(itemData);
                if (itemStack == null) {
                    logger.warn("Failed to deserialize item for key: " + key);
                    continue;
                }

                prizes.put(key, new PrizeData(itemStack));
            }
            return true;
        } catch (Exception ex) {
            logger.error("Unexpected error while loading prizes.yml.", ex);
            return false;
        }
    }

    /**
     * Gets a prize by its key.
     *
     * @param key The prize key.
     * @return The prize data, or null if not found.
     */
    public @Nullable PrizeData get(String key) {
        return prizes.get(key);
    }

    /**
     * Rewards a player with a randomly selected prize from the given chest.
     * Handles item giving, command execution, cooldowns, and usage tracking.
     *
     * @param player The player to reward.
     * @param chest  The chest from which the prize is selected.
     * @return True if the reward was given successfully, false otherwise.
     */
    public boolean reward(Player player, Chest chest) {
        var chat = plugin.chat();
        var database = plugin.database();

        var prizeOpt = chest.getRandomPrize();
        if (prizeOpt.isEmpty()) {
            chat.sendLocalizedMsg(player, "chests.no-prizes-set", Map.of("chest", chest.getKey()));
            return false;
        }

        var prize = prizeOpt.get();
        final PrizeData prizeData = get(prize.getKey());
        if (prizeData == null) {
            chat.sendLocalizedMsg(player, "chests.no-prize-found", Map.of("prize", prize.getKey()));
            return false;
        }

        List<ItemStack> rewards = new ArrayList<>();
        List<String> itemRewards = new ArrayList<>();

        for (var guaranteedPrize : chest.getGuaranteedPrizes()) {
            final PrizeData guaranteedPrizeData = get(guaranteedPrize.getKey());
            if (guaranteedPrizeData == null) {
                logger.warn("Failed to get the prize data for the following key: " + guaranteedPrize.getKey());
                continue;
            }

            var item = guaranteedPrizeData.getItem();
            if (guaranteedPrizeData.getType() == EPrizeType.COMMAND) {
                String command = guaranteedPrizeData.getCommand();
                if (command != null) {
                    command = command.replace("%player%", player.getName())
                            .replace("amount", String.valueOf(next(guaranteedPrize.getMinAmount(), guaranteedPrize.getMaxAmount())));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                continue;
            }
            fillList(item, guaranteedPrize.getMinAmount(), guaranteedPrize.getMaxAmount(), rewards);
        }


        var prizeItem = prizeData.getItem();
        itemRewards.add(AdventureUtils.toMiniMessage(prizeItem.displayName()));
        if (prizeData.getType() == EPrizeType.COMMAND) {
            String command = prizeData.getCommand();
            if (command != null) {
                 command = command.replace("%player%", player.getName())
                         .replace("%amount%", String.valueOf(next(prize.getMinAmount(), prize.getMaxAmount())));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        } else {
            fillList(prizeItem, prize.getMinAmount(), prize.getMaxAmount(), rewards);
        }


        // Iterate through the rewards to determine which reward the player receives.
        final var location = player.getLocation();
        boolean inventoryFullMessageSent = false;
        for (ItemStack item : rewards) {

            var remainder = player.getInventory().addItem(item);
            if (remainder.isEmpty())
                continue;

            if (!inventoryFullMessageSent) {
                chat.sendLocalizedMsg(player, "chests.inventory-full");
                inventoryFullMessageSent = true;
            }

            for (ItemStack remainingItem : remainder.values()) {
                location.getWorld().dropItemNaturally(location, remainingItem);
            }
        }

        // Add a cooldown for the chest to the player's cache and database.
        plugin.scheduler().runAsync(new YggraTask() {
            @Override
            public void run() {
                var cooldown = LocalDateTime.now().plusSeconds(chest.getCooldown());
                database.cooldowns().add(new Cooldown(UUID.randomUUID(), player.getUniqueId(), config.storageContext, ECooldownType.OPEN, chest.getKey(), cooldown));
                var chestUsage = database.getUsage(player.getUniqueId(), config.storageContext, chest.getKey());
                if (chestUsage != null) {
                    chestUsage.updateUsages(chestUsage.getUsages() - 1);
                    database.chestUsages().update(chestUsage.getId(), chestUsage);
                }
            }
        });

        // Notify the player that they have received a reward.
        Map<String, Object> args = Map.of("chest", translator.localize(player, chest.getNameKey()),
                "prizes", String.join(", ", itemRewards));
        chat.sendLocalizedMsg(player, "chests.open-successful", args);

        plugin.title().send(player, "chests.prize-title", args);
        return true;
    }

    /**
     * Splits an item stack into multiple stacks respecting max stack size.
     *
     * @param original The base item stack to distribute.
     * @param min      The minimum amount to give.
     * @param max      The maximum amount to give.
     * @param list     The list to append the resulting stacks to.
     */
    private void fillList(final ItemStack original, int min, int max, List<ItemStack> list) {
        final int maxStackSize = original.getMaxStackSize();
        int remainingAmount = next(min, max);
        while (true) {
            ItemStack clone = original.clone();
            if (remainingAmount - maxStackSize > 0) {
                clone.setAmount(maxStackSize);
                list.add(clone);
                remainingAmount -= maxStackSize;
            }
            else {
                clone.setAmount(remainingAmount);
                list.add(clone);
                break;
            }
        }
    }

    /**
     * Returns a random integer between min and max (inclusive).
     *
     * @param min The minimum value (inclusive).
     * @param max The maximum value (inclusive).
     * @return A random integer within the range.
     */
    private int next(int min, int max) {
        if (min >= max)
            return min;

        if (max < 2)
            return 1;

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
