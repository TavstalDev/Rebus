package com.github.TavstalDev.rebus.models;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemSerializer;
import com.github.TavstalDev.yggraLib.bukkit.items.IItemStackAccessor;
import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import com.github.TavstalDev.yggraLib.bukkit.utils.AdventureUtils;
import com.github.TavstalDev.yggraLib.shadow.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a loot chest with configurable properties, costs, cooldowns, and associated prizes.
 */
@Getter
public class Chest {

    private final String key;
    private final String nameKey;
    private final String loreKey;
    private final Material blockMaterial;
    private final ItemStack displayItem;
    private final double cost;
    private final long cooldown;
    private final long buyCooldown;
    private final String permission;
    private final int slot;
    private final String particle;
    private final int particleCount;
    private final SoundData openSound;
    private final SoundData closeSound;
    private final SoundData completionSound;
    private final boolean isHighTier;
    private final boolean spawnLightningStrike;
    private final HashSet<Prize> guaranteedPrizes;
    private final HashSet<Prize> prizes;
    private final int totalWeight;

    /**
     * Creates a new chest with the given configuration.
     *
     * @param key The unique identifier for the chest.
     * @param nameKey The localization key for the chest name.
     * @param loreKey The localization key for the chest description.
     * @param blockMaterial The material of the chest block.
     * @param displayItem The item displayed in the GUI.
     * @param cost The cost to open the chest.
     * @param cooldown The cooldown time between uses in seconds.
     * @param buyCooldown The cooldown time between purchases in seconds.
     * @param permission The permission required to use the chest.
     * @param slot The GUI slot position.
     * @param particle The particle effect name.
     * @param particleCount The number of particles to display.
     * @param openSound The sound played when opened.
     * @param closeSound The sound played when closed.
     * @param completionSound The sound played on completion.
     * @param isHighTier Whether this is a high-tier chest.
     * @param spawnLightningStrike Whether lightning strikes on opening.
     * @param guaranteedPrizes The set of guaranteed prizes.
     * @param prizes The set of weighted prizes.
     */
    public Chest(String key, String nameKey, String loreKey, Material blockMaterial, ItemStack displayItem,
                 double cost, long cooldown, long buyCooldown, String permission, int slot, String particle,
                 int particleCount, SoundData openSound, SoundData closeSound, SoundData completionSound,
                 boolean isHighTier, boolean spawnLightningStrike, HashSet<Prize> guaranteedPrizes,
                 HashSet<Prize> prizes) {
        this.key = key;
        this.nameKey = nameKey;
        this.loreKey = loreKey;
        this.blockMaterial = blockMaterial;
        this.displayItem = displayItem;
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
        this.spawnLightningStrike = spawnLightningStrike;
        this.guaranteedPrizes = guaranteedPrizes;
        this.prizes = prizes;
        int localWeight = 0;
        for (Prize prize : prizes) {
            localWeight += prize.getWeight();
        }
        this.totalWeight = localWeight;
    }

    /**
     * Gets the display item of the chest with translations applied.
     *
     * @param player The player viewing the item.
     * @param translator The translation service.
     * @param includeGuiDetails Whether to include price, permission, and preview lore lines.
     * @return The translated display item.
     */
    public ItemStack getTranslatedDisplayItem(Player player, ITranslationService translator,
                                              IItemStackAccessor itemStackAccessor, INbtAccessor nbtAccessor,
                                              boolean includeGuiDetails) {
        ItemStack clone = displayItem.clone();
        UUID playerId = player.getUniqueId();
        Component name = AdventureUtils.parse(translator.localize(playerId, getNameKey()), true);
        List<String> rawLore = translator.localizeList(playerId, getLoreKey());
        List<Component> lore = new ArrayList<>();
        String priceLine = includeGuiDetails ? translator.localize(cost > 0 ? "gui.main.price" : "gui.main.free",
                MapUtil.of("price", cost)) : "";
        String permissionLine = includeGuiDetails ?
                translator.localize(player.hasPermission(permission) ? "gui.main.buy-click" : "gui.main.no-permission") : "";
        String previewLine = includeGuiDetails ? translator.localize("gui.main.preview-click") : "";
        for (String line : rawLore) {
            if (line.contains("%price%")) {
                if (!includeGuiDetails)
                    continue;
                line = priceLine;
            }
            if (line.contains("%permission%")) {
                if (!includeGuiDetails)
                    continue;
                line = permissionLine;
            }
            if (line.contains("%preview%")) {
                if (!includeGuiDetails)
                    continue;
                line = previewLine;
            }
            lore.add(AdventureUtils.parse(line, true));
        }

        itemStackAccessor.setDisplayName(clone, name);
        itemStackAccessor.setLoreComp(clone, lore);
        if (includeGuiDetails) {
            // TODO: Replace with DupeService
            nbtAccessor.set(clone, "YGGRA_DUPED", true);
        }
        return clone;
    }

    /**
     * Gets the guaranteed prizes associated with the chest.
     *
     * @return A set of guaranteed prizes.
     */
    public Set<Prize> getGuaranteedPrizes() {
        return Collections.unmodifiableSet(guaranteedPrizes);
    }

    /**
     * Gets the prizes associated with the chest.
     *
     * @return A set of prizes.
     */
    public Set<Prize> getPrizes() {
        return Collections.unmodifiableSet(prizes);
    }

    /**
     * Selects a random prize based on weighted probability.
     *
     * @return An {@link Optional} containing the randomly selected prize, or empty if no prizes are available.
     */
    public Optional<Prize> getRandomPrize() {
        if (prizes.isEmpty() || totalWeight <= 0) {
            return Optional.empty();
        }

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeightSum = 0;
        for (Prize prize : prizes) {
            currentWeightSum += prize.getWeight();
            if (randomValue < currentWeightSum) {
                return Optional.of(prize);
            }
        }
        return Optional.empty();
    }

    public static @Nullable Chest fromConfigurationSection(String key, ConfigurationSection section, IItemSerializer itemSerializer,
                                                           YggraLogger logger) {
        String nameKey = section.getString("nameKey", "");
        String loreKey = section.getString ("loreKey", "");
        Optional<XMaterial> xblockMaterial = XMaterial.matchXMaterial(section.getString("blockMaterial", "CHEST"));
        Material blockMaterial;
        if (xblockMaterial.isPresent())
            blockMaterial = xblockMaterial.get().get();
        else
            blockMaterial = XMaterial.CHEST.get();


        ConfigurationSection displayItemSection = section.getConfigurationSection("displayItem");
        if (displayItemSection == null) {
            logger.warn("Invalid display item for chest: " + key);
            return null;
        }
        ItemStack displayItem = itemSerializer.deserializeItemStack(displayItemSection.getValues(true));
        if (displayItem == null) {
            logger.warn("Failed to deserialize display item for chest: " + key);
            return null;
        }

        double cost = section.getDouble("cost", 0.0);
        long cooldown = section.getLong("cooldown", 0L);
        long buyCooldown = section.getLong("buyCooldown", 0L);
        String permission = section.getString("permission", "rebus.use");
        int slot = section.getInt("slot", 0);

        String particle = section.getString("particle", "ENCHANT");
        int particleCount = section.getInt("particleCount", 30);

        ConfigurationSection soundSection = section.getConfigurationSection("sounds.open");
        SoundData openSound = getSoundData(soundSection, "block.chest.open");
        soundSection = section.getConfigurationSection("sounds.close");
        SoundData closeSound = getSoundData(soundSection, "block.chest.close");
        soundSection = section.getConfigurationSection("sounds.completion");
        SoundData completionSound = getSoundData(soundSection, "entity.player.level_up");

        boolean isHighTier = section.getBoolean("isHighTier", false);
        boolean spawnLightningStrike = section.getBoolean("spawnLightningStrike", false);

        HashSet<Prize> guaranteedPrizes = new LinkedHashSet<>();
        HashSet<Prize> prizes = new LinkedHashSet<>();

        for (Map<?, ?> rawEntry : section.getMapList("guaranteedPrizes")) {
            Map<String, Object> prizeMap = toStringObjectMap(rawEntry);
            Prize prize = Prize.fromMap(prizeMap, logger);
            if (prize != null) {
                guaranteedPrizes.add(prize);
            }
        }

        for (Map<?, ?> rawEntry : section.getMapList("prizes")) {
            Map<String, Object> prizeMap = toStringObjectMap(rawEntry);
            Prize prize = Prize.fromMap(prizeMap, logger);
            if (prize != null) {
                prizes.add(prize);
            }
        }

        return new Chest(key, nameKey, loreKey, blockMaterial, displayItem, cost, cooldown, buyCooldown, permission, slot, particle, particleCount,
                openSound, closeSound, completionSound, isHighTier, spawnLightningStrike, guaranteedPrizes, prizes);
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (rawMap == null) {
            return result;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private static SoundData getSoundData(ConfigurationSection section, String defaultName) {
        SoundData openSound;
        if (section != null) {
            String soundName = getSound(section.getString("name", defaultName));
            float volume = section.getFloat("volume", 1.0f);
            float pitch = section.getFloat("pitch", 1.0f);
            openSound = new SoundData(soundName, volume, pitch);
        }
        else
            openSound = new SoundData(defaultName);
        return openSound;
    }

    private static String getSound(String input) {
        if (input == null || input.isEmpty())
            return null;

        Optional<XSound> sound = XSound.of(input);
        if (!sound.isPresent())
            return null;

        Sound s = sound.get().get();
        if (s == null)
            return null;
        return s.name();
    }
}
