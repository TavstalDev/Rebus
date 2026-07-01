package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.rebus.util.IconUtils;
import io.github.tavstaldev.yggra.core.gui.GuiDupeDetector;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
import io.github.tavstaldev.yggra.core.services.TranslationService;
import io.github.tavstaldev.yggra.items.api.ItemSerializerBase;
import io.github.tavstaldev.yggra.items.api.utils.AdventureUtils;
import io.github.tavstaldev.yggra.items.api.utils.TypeUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
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
    private final String openSound;
    private final String closeSound;
    private final String completionSound;
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
    public Chest(String key, String nameKey, String loreKey, Material blockMaterial, ItemStack displayItem, double cost, long cooldown, long buyCooldown,
                 String permission, int slot, String particle, int particleCount, String openSound, String closeSound, String completionSound,
                 boolean isHighTier, boolean spawnLightningStrike, HashSet<Prize> guaranteedPrizes, HashSet<Prize> prizes) {
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
        for (var prize : prizes) {
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
    public ItemStack getTranslatedDisplayItem(Player player, TranslationService translator, boolean includeGuiDetails) {
        var clone = displayItem.clone();
        var name = AdventureUtils.parse(translator.localize(player, getNameKey()), true);
        var rawLore = translator.localizeList(player, getLoreKey());
        List<Component> lore = new ArrayList<>();
        String priceLine = includeGuiDetails ? translator.localize(cost > 0 ? "gui.main.price" : "gui.main.free", Map.of("price", cost)) : "";
        String permissionLine = includeGuiDetails ? translator.localize(player.hasPermission(permission) ? "gui.main.buy-click" : "gui.main.no-permission") : "";
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
        clone.editMeta(x -> {
            x.displayName(name);
            x.lore(lore);
            if (includeGuiDetails)
                x.getPersistentDataContainer().set(GuiDupeDetector.getDupeProtectedKey(), PersistentDataType.BOOLEAN, true);
        });
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

    /**
     * Creates a chest from a configuration map.
     *
     * @param key The chest key.
     * @param map The configuration map containing chest data.
     * @param itemSerializer The serializer for deserializing display items.
     * @param logger The logger for error reporting.
     * @return The parsed chest, or null if parsing fails.
     */
    public static @Nullable Chest fromMap(String key, Map<String, Object> map, ItemSerializerBase itemSerializer, YggraLogger logger) {
        String nameKey = (String) map.getOrDefault("nameKey", "");
        String loreKey = (String) map.getOrDefault("loreKey", "");
        Material blockMaterial = IconUtils.getMaterial((String) map.getOrDefault("blockMaterial", "CHEST"));
        Map<String, Object> rawDisplayItem = TypeUtils.castAsMap(map.get("displayItem"), null);
        if (rawDisplayItem == null) {
            logger.warn("Invalid display item for chest: " + map.get("key"));
            return null;
        }
        ItemStack displayItem = itemSerializer.deserializeItemStack(rawDisplayItem);
        if (displayItem == null) {
            logger.warn("Failed to deserialize display item for chest: " + map.get("key"));
            return null;
        }

        double cost = ((Number) map.getOrDefault("cost", 0)).doubleValue();
        long cooldown = ((Number) map.getOrDefault("cooldown", 0)).longValue();
        long buyCooldown = ((Number) map.getOrDefault("buyCooldown", 0)).longValue();
        String permission = (String) map.getOrDefault("permission", "rebus.use");
        int slot = ((Number) map.getOrDefault("slot", 0)).intValue();
        String particle = (String) map.getOrDefault("particle", "ENCHANT");
        int particleCount = ((Number) map.getOrDefault("particleCount", 30)).intValue();
        String openSound = (String) map.getOrDefault("openSound", "minecraft:block.chest.open");
        String closeSound = (String) map.getOrDefault("closeSound", "minecraft:block.chest.close");
        String completionSound = (String) map.getOrDefault("completionSound", "minecraft:entity.player.level_up");
        boolean isHighTier = (Boolean) map.getOrDefault("isHighTier", false);
        boolean spawnLightningStrike = (Boolean)map.getOrDefault("spawnLightningStrike", false);

        HashSet<Prize> guaranteedPrizes = new LinkedHashSet<>();
        HashSet<Prize> prizes = new LinkedHashSet<>();

        List<Map<String, Object>> guaranteedPrizesList = TypeUtils.castAsList(map.get("guaranteedPrizes"), null);
        if (guaranteedPrizesList != null) {
            for (Map<String, Object> prizeMap : guaranteedPrizesList) {
                Prize prize = Prize.fromMap(prizeMap, logger);
                if (prize != null) {
                    guaranteedPrizes.add(prize);
                }
            }
        }

        List<Map<String, Object>> prizesList = TypeUtils.castAsList(map.get("prizes"), null);
        if (prizesList != null) {
            for (Map<String, Object> prizeMap : prizesList) {
                Prize prize = Prize.fromMap(prizeMap, logger);
                if (prize != null) {
                    prizes.add(prize);
                }
            }
        }

        return new Chest(key, nameKey, loreKey, blockMaterial, displayItem, cost, cooldown, buyCooldown, permission, slot, particle, particleCount,
                openSound, closeSound, completionSound, isHighTier, spawnLightningStrike, guaranteedPrizes, prizes);
    }
}
