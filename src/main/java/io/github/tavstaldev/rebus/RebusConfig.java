package io.github.tavstaldev.rebus;

import io.github.tavstaldev.minecorelib.config.ConfigurationBase;
import io.github.tavstaldev.rebus.util.IconUtils;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RebusConfig extends ConfigurationBase {
    public RebusConfig() {
        super(Rebus.Instance, "config.yml", null);
    }

    public String prefix;
    public boolean checkForUpdates, debug;

    public String storageType, storageContext, storageFilename, storageHost, storageDatabase, storageUsername, storagePassword, storageTablePrefix;
    public int storagePort;

    public String npcName, npcSkin;

    public boolean guiFillEmptySlots;
    public Material guiPlaceholderMaterial, guiCloseMaterial;
    public int guiRows, guiCloseBtnSlot;

    @Override
    protected void loadDefaults() {
        // General
        resolve("locale", "hun");
        resolve("usePlayerLocale", false);
        checkForUpdates = resolveGet("checkForUpdates", false);
        debug = resolveGet("debug", false);
        prefix = resolveGet("prefix", "&dRébusz &8»");

        // Storage
        storageType = resolveGet("storage.type", "sqlite");
        storageContext = resolveGet("storage.context", "skypvp");
        storageFilename = resolveGet("storage.filename", "database");
        storageHost = resolveGet("storage.host", "localhost");
        storagePort = resolveGet("storage.port", 3306);
        storageDatabase = resolveGet("storage.database", "minecraft");
        storageUsername = resolveGet("storage.username", "root");
        storagePassword = resolveGet("storage.password", "ascent");
        storageTablePrefix = resolveGet("storage.tablePrefix", "rebus");

        // npc
        npcName = resolveGet("npc.name", "&d&l_Rébusz_");
        npcSkin = resolveGet("npc.skin", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjgzOTMwZjcxYmYyNWRkMTNiMzY0ZmY3ZTBlODdlODhiODc1NmNiYmJmODIyNDEwZjQ3MDQ1ZWNmMTI3NjM5OSJ9fX0=");

        // gui
        guiFillEmptySlots = resolveGet("gui.fillEmptySlots", true);
        String material = resolveGet("gui.placeholderMaterial", "BLACK_STAINED_GLASS_PANE");
        guiPlaceholderMaterial = IconUtils.getMaterial(material);
        material = resolveGet("gui.closeMaterial", "BARRIER");
        guiCloseMaterial = IconUtils.getMaterial(material);
        guiRows = resolveGet("gui.rows", 1);
        guiCloseBtnSlot = resolveGet("gui.closeBtnSlot", 8);

        // chests
        //#region Daily chest
        Map<String, Object> dailyChest = new HashMap<>();
        dailyChest.put("name", "&aNapi láda");
        dailyChest.put("description", Arrays.asList("&7Ingyenes napi láda mindenkinek."));
        dailyChest.put("material", "CHEST");
        dailyChest.put("cost", 0);
        dailyChest.put("cooldown", 86400);
        dailyChest.put("permission", "rebus.chest.daily");
        dailyChest.put("slot", 0);
        dailyChest.put("particle", "ENCHANT");
        dailyChest.put("particleCount", 30);
        dailyChest.put("openSound", "BLOCK_CHEST_OPEN");
        dailyChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        dailyChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        dailyChest.put("isHighTier", false);
        //#endregion
        //#region common chest
        Map<String, Object> commonChest = new HashMap<>();
        commonChest.put("name", "&6Alap láda");
        commonChest.put("description", Arrays.asList("&7Kezdő kalandozók számára."));
        commonChest.put("material", "TRAPPED_CHEST");
        commonChest.put("cost", 100);
        commonChest.put("cooldown", 86400);
        commonChest.put("permission", "rebus.chest.default");
        commonChest.put("slot", 2);
        commonChest.put("particle", "HAPPY_VILLAGER");
        commonChest.put("particleCount", 35);
        commonChest.put("openSound", "BLOCK_CHEST_OPEN");
        commonChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        commonChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        commonChest.put("isHighTier", false);
        //#endregion
        //#region uncommon chest
        Map<String, Object> uncommonChest = new HashMap<>();
        uncommonChest.put("name", "&5Pandora szelencéje");
        uncommonChest.put("description", Arrays.asList("&7Tapasztalt játékosok számára."));
        uncommonChest.put("material", "ENDER_CHEST");
        uncommonChest.put("cost", 250);
        uncommonChest.put("cooldown", 86400);
        uncommonChest.put("permission", "rebus.chest.pandora");
        uncommonChest.put("slot", 4);
        uncommonChest.put("particle", "FIREWORK");
        uncommonChest.put("particleCount", 40);
        uncommonChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        uncommonChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        uncommonChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        uncommonChest.put("isHighTier", true);
        //#endregion
        //#region rare chest
        Map<String, Object> rareChest = new HashMap<>();
        rareChest.put("name", "&cKiválasztottak ládája");
        rareChest.put("description", Arrays.asList("&7Veterán kalandorok számára"));
        rareChest.put("material", "BEACON");
        rareChest.put("cost", 500);
        rareChest.put("cooldown", 86400);
        rareChest.put("permission", "rebus.chest.choosen");
        rareChest.put("slot", 6);
        rareChest.put("particle", "ENCHANTED_HIT");
        rareChest.put("particleCount", 50);
        rareChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        rareChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        rareChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        rareChest.put("isHighTier", true);
        //#endregion
        //#region epic chest
        Map<String, Object> epicChest = new HashMap<>();
        epicChest.put("name", "&6Alap láda");
        epicChest.put("description", Arrays.asList("&7Kezdő kalandozók számára."));
        epicChest.put("material", "TRAPPED_CHEST");
        epicChest.put("cost", 100);
        epicChest.put("cooldown", 86400);
        epicChest.put("permission", "rebus.chest.default");
        epicChest.put("slot", 2);
        epicChest.put("particle", "HAPPY_VILLAGER");
        epicChest.put("particleCount", 35);
        epicChest.put("openSound", "BLOCK_CHEST_OPEN");
        epicChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        epicChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        epicChest.put("isHighTier", false);
        //#endregion
        //#region legendary chest
        Map<String, Object> legendaryChest = new HashMap<>();
        legendaryChest.put("name", "&5Pandora szelencéje");
        legendaryChest.put("description", Arrays.asList("&7Tapasztalt játékosok számára."));
        legendaryChest.put("material", "ENDER_CHEST");
        legendaryChest.put("cost", 250);
        legendaryChest.put("cooldown", 86400);
        legendaryChest.put("permission", "rebus.chest.pandora");
        legendaryChest.put("slot", 4);
        legendaryChest.put("particle", "FIREWORK");
        legendaryChest.put("particleCount", 40);
        legendaryChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        legendaryChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        legendaryChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        legendaryChest.put("isHighTier", true);
        //#endregion
        resolve("chests", Map.of(
                "daily", dailyChest,
                "commonchest", commonChest,
                "uncommonchest", uncommonChest,
                "rarechest", rareChest,
                "epicchest", epicChest,
                "legendarychest", legendaryChest
        ));
    }
}
