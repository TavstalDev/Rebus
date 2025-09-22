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
        dailyChest.put("buyCooldown", 86400);
        dailyChest.put("permission", "rebus.chest.daily");
        dailyChest.put("slot", 0);
        dailyChest.put("particle", "ENCHANT");
        dailyChest.put("particleCount", 30);
        dailyChest.put("openSound", "BLOCK_CHEST_OPEN");
        dailyChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        dailyChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        dailyChest.put("isHighTier", false);
        //#endregion
        //#region Default chest
        Map<String, Object> bronzeChest = new HashMap<>();
        bronzeChest.put("name", "&6Alap láda");
        bronzeChest.put("description", Arrays.asList("&7Kezdő kalandozók számára.", "&7Garantált mentőrudak: &e1"));
        bronzeChest.put("material", "TRAPPED_CHEST");
        bronzeChest.put("cost", 500);
        bronzeChest.put("cooldown", 300);
        bronzeChest.put("buyCooldown", 300);
        bronzeChest.put("permission", "rebus.chest.default");
        bronzeChest.put("slot", 2);
        bronzeChest.put("particle", "HAPPY_VILLAGER");
        bronzeChest.put("particleCount", 35);
        bronzeChest.put("openSound", "BLOCK_CHEST_OPEN");
        bronzeChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        bronzeChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        bronzeChest.put("isHighTier", false);
        //#endregion
        //#region Pandora chest
        Map<String, Object> silverChest = new HashMap<>();
        silverChest.put("name", "&5Pandora szelencéje");
        silverChest.put("description", Arrays.asList("&7Tapasztalt játékosok számára.", "&7Garantált mentőrudak: &e4"));
        silverChest.put("material", "ENDER_CHEST");
        silverChest.put("cost", 750);
        silverChest.put("cooldown", 300);
        silverChest.put("buyCooldown", 300);
        silverChest.put("permission", "rebus.chest.pandora");
        silverChest.put("slot", 4);
        silverChest.put("particle", "FIREWORK");
        silverChest.put("particleCount", 40);
        silverChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        silverChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        silverChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        silverChest.put("isHighTier", true);
        //#endregion
        //#region Choosen chest
        Map<String, Object> goldChest = new HashMap<>();
        goldChest.put("name", "&cKiválasztottak ládája");
        goldChest.put("description", Arrays.asList("&7Veterán kalandorok számára", "&7Garantált mentőrudak: &e10"));
        goldChest.put("material", "BEACON");
        goldChest.put("cost", 1000);
        goldChest.put("cooldown", 300);
        goldChest.put("buyCooldown", 300);
        goldChest.put("permission", "rebus.chest.choosen");
        goldChest.put("slot", 6);
        goldChest.put("particle", "ENCHANTED_HIT");
        goldChest.put("particleCount", 50);
        goldChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        goldChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        goldChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        goldChest.put("isHighTier", true);
        //#endregion
        resolve("chests", Map.of(
                "daily", dailyChest,
                "default", bronzeChest,
                "pandora", silverChest,
                "choosen", goldChest
        ));
    }
}
