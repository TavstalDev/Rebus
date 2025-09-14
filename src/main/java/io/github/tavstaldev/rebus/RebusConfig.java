package io.github.tavstaldev.rebus;

import com.cryptomorin.xseries.particles.XParticle;
import io.github.tavstaldev.minecorelib.config.ConfigurationBase;
import io.github.tavstaldev.rebus.util.IconUtils;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        prefix = resolveGet("prefix", "&dRebus &8»");

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
        npcName = resolveGet("npc.name", "&d&l_Rebus_");
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
        dailyChest.put("name", "&aNapi Láda");
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
        //#region Bronze chest
        Map<String, Object> bronzeChest = new HashMap<>();
        bronzeChest.put("name", "&6Bronz Láda");
        bronzeChest.put("description", Arrays.asList("&7Kezdő kalandozók számára."));
        bronzeChest.put("material", "TRAPPED_CHEST");
        bronzeChest.put("cost", 100);
        bronzeChest.put("cooldown", 86400);
        bronzeChest.put("permission", "rebus.chest.bronze");
        bronzeChest.put("slot", 1);
        bronzeChest.put("particle", "HAPPY_VILLAGER");
        bronzeChest.put("particleCount", 35);
        bronzeChest.put("openSound", "BLOCK_CHEST_OPEN");
        bronzeChest.put("closeSound", "BLOCK_CHEST_CLOSE");
        bronzeChest.put("completionSound", "ENTITY_PLAYER_LEVELUP");
        bronzeChest.put("isHighTier", false);
        //#endregion
        //#region Silver chest
        Map<String, Object> silverChest = new HashMap<>();
        silverChest.put("name", "&fEzüst Láda");
        silverChest.put("description", Arrays.asList("&7Tapasztalt játékosok számára."));
        silverChest.put("material", "ENDER_CHEST");
        silverChest.put("cost", 250);
        silverChest.put("cooldown", 86400);
        silverChest.put("permission", "rebus.chest.silver");
        silverChest.put("slot", 2);
        silverChest.put("particle", "FIREWORK");
        silverChest.put("particleCount", 40);
        silverChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        silverChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        silverChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        silverChest.put("isHighTier", true);
        //#endregion
        //#region Gold chest
        Map<String, Object> goldChest = new HashMap<>();
        goldChest.put("name", "&eArany Láda");
        goldChest.put("description", Arrays.asList("&7Veterán kalandorok számára"));
        goldChest.put("material", "SHULKER_BOX");
        goldChest.put("cost", 500);
        goldChest.put("cooldown", 86400);
        goldChest.put("permission", "rebus.chest.gold");
        goldChest.put("slot", 3);
        goldChest.put("particle", "ENCHANTED_HIT");
        goldChest.put("particleCount", 50);
        goldChest.put("openSound", "BLOCK_ENDER_CHEST_OPEN");
        goldChest.put("closeSound", "BLOCK_ENDER_CHEST_CLOSE");
        goldChest.put("completionSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        goldChest.put("isHighTier", true);
        //#endregion
        //#region Diamond chest
        Map<String, Object> diamondChest = new HashMap<>();
        diamondChest.put("name", "&bGyémánt Láda");
        diamondChest.put("description", Arrays.asList("&7Elit harcosok számára."));
        diamondChest.put("material", "BEACON");
        diamondChest.put("cost", 1000);
        diamondChest.put("cooldown", 86400);
        diamondChest.put("permission", "rebus.chest.diamond");
        diamondChest.put("slot", 4);
        diamondChest.put("particle", "END_ROD");
        diamondChest.put("particleCount", 60);
        diamondChest.put("openSound", "BLOCK_BEACON_ACTIVATE");
        diamondChest.put("closeSound", "BLOCK_BEACON_POWER_SELECT");
        diamondChest.put("completionSound", "ITEM_ARMOR_EQUIP_DIAMOND");
        diamondChest.put("isHighTier", true);
        //#endregion
        //#region Netherite chest
        Map<String, Object> netheriteChest = new HashMap<>();
        netheriteChest.put("name", "&8Netherite Láda");
        netheriteChest.put("description", Arrays.asList("&7Alvilági hősök számára."));
        netheriteChest.put("material", "CRYING_OBSIDIAN");
        netheriteChest.put("cost", 2500);
        netheriteChest.put("cooldown", 86400);
        netheriteChest.put("permission", "rebus.chest.netherite");
        netheriteChest.put("slot", 5);
        netheriteChest.put("particle", "FLAME");
        netheriteChest.put("particleCount", 75);
        netheriteChest.put("openSound", "ENTITY_BLAZE_SHOOT");
        netheriteChest.put("closeSound", "ENTITY_BLAZE_AMBIENT");
        netheriteChest.put("completionSound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        netheriteChest.put("isHighTier", true);
        //#endregion
        //#region Mythic chest
        Map<String, Object> mythicChest = new HashMap<>();
        mythicChest.put("name", "&5Mitikus Láda");
        mythicChest.put("description", Arrays.asList("&7A legnagyobb bajnokok számára."));
        mythicChest.put("material", "DRAGON_EGG");
        mythicChest.put("cost", 5000);
        mythicChest.put("cooldown", 86400);
        mythicChest.put("permission", "rebus.chest.mythic");
        mythicChest.put("slot", 6);
        mythicChest.put("particle", "DRAGON_BREATH");
        mythicChest.put("particleCount", 100);
        mythicChest.put("openSound", "ENTITY_WITHER_SPAWN");
        mythicChest.put("closeSound", "ENTITY_DRAGON_FIREBALL_EXPLODE");
        mythicChest.put("completionSound", "ENTITY_WITHER_SPAWN");
        mythicChest.put("isHighTier", true);
        //#endregion
        resolve("chests", Map.of(
                "daily", dailyChest,
                "bronze", bronzeChest,
                "silver", silverChest,
                "gold", goldChest,
                "diamond", diamondChest,
                "netherite", netheriteChest,
                "mythic", mythicChest

        ));
    }
}
