package com.github.TavstalDev.rebus.services.prize;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.database.models.ChestUsage;
import com.github.TavstalDev.rebus.database.models.Cooldown;
import com.github.TavstalDev.rebus.database.models.ECooldownType;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.models.EPrizeType;
import com.github.TavstalDev.rebus.models.Prize;
import com.github.TavstalDev.rebus.models.PrizeData;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemSerializer;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import com.github.TavstalDev.yggraLib.bukkit.utils.AdventureUtils;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.file.FileConfiguration;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import com.github.TavstalDev.yggraLib.shadow.yggraTitle.api.ITitle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PrizeService implements IPrizeService {
    private final Rebus plugin;
    private final RebusConfig config;
    private final YggraLogger logger;
    private final ITranslationService translator;
    private final IItemSerializer itemSerializer;
    private final IItemStackAccessor itemStackAccessor;
    private final IChatService chatService;
    private final ITitle titleService;
    private final IRebusDatabase database;
    private final IScheduler scheduler;
    private final HashMap<String, PrizeData> prizes;
    private final File prizesFile;
    private final FileConfiguration yamlFile;

    public PrizeService(Rebus plugin, ITranslationService translator, IItemSerializer itemSerializer,
                        IItemStackAccessor itemStackAccessor, IChatService chatService, ITitle titleService,
                        IRebusDatabase database, IScheduler scheduler) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.logger = plugin.logger().withModule(PrizeService.class);
        this.translator = translator;
        this.itemSerializer = itemSerializer;
        this.itemStackAccessor = itemStackAccessor;
        this.chatService = chatService;
        this.titleService = titleService;
        this.database = database;
        this.scheduler = scheduler;
        this.prizes = new HashMap<>();
        this.prizesFile = Paths.get(plugin.getDataFolder().getPath(), "prizes.yml").toFile();
        this.yamlFile = new YamlConfiguration();
    }

    /** {@inheritDoc} */
    public boolean load() {
        prizes.clear();

        // Copy the prizes.yml file from resources if it doesn't exist.
        if (!prizesFile.exists()) {
            plugin.saveResource("prizes.yml", false);
        }

        try {
            yamlFile.load(prizesFile);

            ConfigurationSection commandsRoot = yamlFile.getConfigurationSection("data.commands");
            if (commandsRoot != null)
                for (String commandKey : commandsRoot.getKeys(false)) {
                    String command = commandsRoot.getString(commandKey + ".command", "");
                    ConfigurationSection commandElem = commandsRoot.getConfigurationSection(commandKey + ".display");
                    if (commandElem == null) {
                        logger.warn("Missing display field for '" + commandKey + "' command.");
                        continue;
                    }

                    Map<String, Object> displayItemData = commandElem.getValues(true);
                    if (displayItemData.isEmpty()) {
                        logger.warn("Invalid display item for command key '" + commandKey + "'.");
                        continue;
                    }

                    ItemStack itemStack = itemSerializer.deserializeItemStack(displayItemData);
                    if (itemStack == null) {
                        logger.warn("Failed to deserialize item for command key: " + commandKey);
                        continue;
                    }
                    prizes.put(commandKey, new PrizeData(command, itemStack));
                }

            ConfigurationSection itemsRoot = yamlFile.getConfigurationSection("data.items");
            if (itemsRoot != null)
                for (String itemKey : itemsRoot.getKeys(false)) {
                    ConfigurationSection itemElem = itemsRoot.getConfigurationSection(itemKey);
                    if (itemElem == null) {
                        logger.warn("Invalid data found for '" + itemKey + "' item.");
                        continue;
                    }

                    Map<String, Object> itemData = new HashMap<>(itemElem.getValues(true));
                    if (itemData.isEmpty()) {
                        logger.warn("Empty data for key '" + itemKey + "' item.");
                        continue;
                    }

                    if (!itemData.containsKey("amount"))
                        itemData.put("amount", 1);

                    ItemStack itemStack = itemSerializer.deserializeItemStack(itemData);
                    if (itemStack == null) {
                        logger.warn("Failed to deserialize item for key: " + itemKey);
                        continue;
                    }

                    prizes.put(itemKey, new PrizeData(itemStack));
                }
            return true;
        }
        catch (Exception ex) {
            logger.error("Failed to load prizes.yml", ex);
            return false;
        }
    }

    /** {@inheritDoc} */
    public @Nullable PrizeData get(String key) {
        return prizes.get(key);
    }

    /** {@inheritDoc} */
    public boolean reward(Player player, Chest chest) {
        UUID playerId = player.getUniqueId();
        Optional<Prize> prizeOpt = chest.getRandomPrize();
        if (!prizeOpt.isPresent()) {
            chatService.sendLocalizedMsg(player, "chests.no-prizes-set", MapUtil.of("chest", chest.getKey()));
            return false;
        }

        Prize prize = prizeOpt.get();
        final PrizeData prizeData = get(prize.getKey());
        if (prizeData == null) {
            chatService.sendLocalizedMsg(player, "chests.no-prize-found", MapUtil.of("prize", prize.getKey()));
            return false;
        }

        List<ItemStack> rewards = new ArrayList<>();
        List<String> itemRewards = new ArrayList<>();

        for (Prize guaranteedPrize : chest.getGuaranteedPrizes()) {
            final PrizeData guaranteedPrizeData = get(guaranteedPrize.getKey());
            if (guaranteedPrizeData == null) {
                logger.warn("Failed to get the prize data for the following key: " + guaranteedPrize.getKey());
                continue;
            }

            ItemStack item = guaranteedPrizeData.getItem();
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


        ItemStack prizeItem = prizeData.getItem();
        itemRewards.add(AdventureUtils.toMiniMessage(
                itemStackAccessor.getDisplayNameComp(prizeItem)));
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
        final Location location = player.getLocation();
        boolean inventoryFullMessageSent = false;
        for (ItemStack item : rewards) {

            HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(item);
            if (remainder.isEmpty())
                continue;

            if (!inventoryFullMessageSent) {
                chatService.sendLocalizedMsg(player, "chests.inventory-full");
                inventoryFullMessageSent = true;
            }

            for (ItemStack remainingItem : remainder.values()) {
                location.getWorld().dropItemNaturally(location, remainingItem);
            }
        }

        // Add a cooldown for the chest to the player's cache and database.
        scheduler.runAsync(new YggraTask() {
            @Override
            public void run() {
                LocalDateTime cooldown = LocalDateTime.now().plusSeconds(chest.getCooldown());
                database.cooldowns().add(new Cooldown(UUID.randomUUID(), playerId,
                        config.storage.getContext(), ECooldownType.OPEN, chest.getKey(), cooldown));
                ChestUsage chestUsage = database.getUsage(playerId, config.storage.getContext(), chest.getKey());
                if (chestUsage != null) {
                    chestUsage.updateUsages(chestUsage.getUsages() - 1);
                    database.chestUsages().update(chestUsage.getId(), chestUsage);
                }
            }
        });

        // Notify the player that they have received a reward.
        Map<String, Object> args = MapUtil.of("chest", translator.localize(playerId, chest.getNameKey()),
                "prizes", String.join(", ", itemRewards));
        chatService.sendLocalizedMsg(player, "chests.open-successful", args);

        String titleMain = translator.localize(playerId, "chests.prize-title.title", args);
        String titleSub = translator.localize(playerId, "chests.prize-title.subtitle", args);
        titleService.send(player, titleMain, titleSub);
        return true;
    }

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

    private int next(int min, int max) {
        if (min >= max)
            return min;

        if (max < 2)
            return 1;

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
