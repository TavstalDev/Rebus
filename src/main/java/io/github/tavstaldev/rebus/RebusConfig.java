package io.github.tavstaldev.rebus;

import io.github.tavstaldev.minecorelib.config.ConfigurationBase;
import io.github.tavstaldev.rebus.util.IconUtils;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RebusConfig extends ConfigurationBase {
    public RebusConfig() {
        super(Rebus.Instance, "config.yml", null);
    }

    // General
    public String prefix;
    public boolean checkForUpdates, debug;

    // Storage
    public String storageType, storageContext, storageFilename, storageHost, storageDatabase, storageUsername, storagePassword, storageTablePrefix;
    public int storagePort;

    // NPC
    public String npcName, npcSkin;

    // GUI
    public boolean guiFillEmptySlots;
    public Material guiPlaceholderMaterial, guiCloseMaterial;
    public int guiRows, guiCloseBtnSlot;

    @Override
    protected void loadDefaults() {
        // General
        resolve("locale", "eng");
        resolve("usePlayerLocale", true);
        checkForUpdates = resolveGet("checkForUpdates", true);
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
        Map<String, Object> dailyChest = new LinkedHashMap<>();
        dailyChest.put("name", "&fNapi láda");
        dailyChest.put("description", Arrays.asList(
                "&eIngyenes napi láda, minden játékos számára",
                "&eegyaránt elérhető.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 10x Kenyér",
                " &e‣ 10x Alma",
                " &e‣ 10x Tölgyfa rönk",
                " &e‣ 1x Kőbalta",
                " &e‣ 1x Horgászbot",
                " &e‣ 1x Íj",
                " &e‣ 16x Nyíl",
                " &e‣ 10x Csont",
                " &e‣ 10x Szén érc",
                " &e‣ 1x Aranyrúd",
                " &e‣ 1x Vasrúd",
                " &e‣ 10x Búzamag",
                " &e‣ 5x Cukornád",
                " &e‣ Véletlenszerű fa eszköz",
                " &e‣ Véletlenszerű bőrpáncél darab"
        ));
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
        //#region common chest
        Map<String, Object> commonChest = new LinkedHashMap<>();
        commonChest.put("name", "&eKözönséges Láda");
        commonChest.put("description", Arrays.asList(
                "&eMegvásárolható láda.",
                "&e5 darab kezdő minőségű tárgyat tartalmaz.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 16x Steak &8&o(10%)",
                " &e‣ 5x Aranyalma &8&o(3%)",
                " &e‣ 10x Fehér gyapjú &8&o(7%)",
                " &e‣ 2x Obszidián &8&o(5%)",
                " &e‣ 16x Üveg &8&o(7%)",
                " &e‣ 1x Vaskard &8&o(7%)",
                " &e‣ 1x Vas csákány &8&o(7%)",
                " &e‣ 1x Vas balta &8&o(7%)",
                " &e‣ 1x Pajzs &8&o(9%)",
                " &e‣ 1x Eldobható gyengeség bájital &8&o(5%)",
                " &e‣ 10x Lazurit &8&o(7%)",
                " &e‣ 5x Aranyérc &8&o(7%)",
                " &e‣ 5x Vasérc &8&o(7%)",
                " &e‣ 1x Cédula &8&o(7%)",
                " &e‣ Véletlenszerű vaspáncél darab &8&o(5%)"
        ));
        commonChest.put("material", "TRAPPED_CHEST");
        commonChest.put("cost", 100);
        commonChest.put("cooldown", 5);
        commonChest.put("buyCooldown", 300);
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
        Map<String, Object> uncommonChest = new LinkedHashMap<>();
        uncommonChest.put("name", "&2Szokatlan Láda");
        uncommonChest.put("description", Arrays.asList(
                "&7Megvásárolható láda.",
                "&75 darab fejlettebb minőségű tárgyat tartalmaz.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 3x Őrláng rúd &8&o(9%)",
                " &e‣ 1x Főzőállvány &8&o(7%)",
                " &e‣ 1x Hatékonyság I könyv &8&o(6%)",
                " &e‣ 1x Törhetetlenség I könyv &8&o(6%)",
                " &e‣ 1x Bányász szerencse I könyv &8&o(6%)",
                " &e‣ 1x Zsákmányolás III könyv &8&o(3%)",
                " &e‣ 2x Végzetgyöngy &8&o(5%)",
                " &e‣ 5x Tűzijáték &8&o(7%)",
                " &e‣ 5x Széllöket &8&o(6%)",
                " &e‣ 1x Nyereg &8&o(9%)",
                " &e‣ 20x Lazurit &8&o(8%)",
                " &e‣ 20x Aranyérc &8&o(8%)",
                " &e‣ 24x Vasérc &8&o(8%)",
                " &e‣ 10x Smaragd &8&o(8%)",
                " &e‣ 1x Varázsló asztal &8&o(4%)"
        ));
        uncommonChest.put("material", "ENDER_CHEST");
        uncommonChest.put("cost", 250);
        uncommonChest.put("cooldown", 5);
        uncommonChest.put("buyCooldown", 300);
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
        Map<String, Object> rareChest = new LinkedHashMap<>();
        rareChest.put("name", "&9Ritka Láda");
        rareChest.put("description", Arrays.asList(
                "&eMegvásárolható láda.",
                "&e5 darab ritka tárgyat tartalmaz.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 5x Gyémánt &8&o(7%)",
                " &e‣ 1x Netherit darab &8&o(5%)",
                " &e‣ 10x Obszidián &8&o(9%)",
                " &e‣ 2x Smaragdblokk &8&o(7%)",
                " &e‣ 5x Vasblokk &8&o(8%)",
                " &e‣ 5x Aranyblokk &8&o(8%)",
                " &e‣ 5x Lazuritblokk &8&o(8%)",
                " &e‣ 1x Gyémántbalta &8&o(4%)",
                " &e‣ 1x Törhetetlenség II könyv &8&o(4%)",
                " &e‣ 1x Bányász szerencse II könyv &8&o(4%)",
                " &e‣ 1x Tüskék II könyv &8&o(4%)",
                " &e‣ 1x Halhatatlanság totem &8&o(7%)",
                " &e‣ 1x Shulker Doboz &8&o(3%)",
                " &e‣ 1x Varázsló asztal &8&o(4%)",
                " &e‣ Véletlenszerű gyémántpáncél darab &8&o(4x4%)"
        ));
        rareChest.put("material", "BEACON");
        rareChest.put("cost", 500);
        rareChest.put("cooldown", 5);
        rareChest.put("buyCooldown", 300);
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
        Map<String, Object> epicChest = new LinkedHashMap<>();
        epicChest.put("name", "&5Epikus Láda");
        epicChest.put("description", Arrays.asList(
                "&eMegvásárolható láda.",
                "&e5 darab erős tárgyat tartalmaz.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 3x OP aranyalma &8&o(4%)",
                " &e‣ 3x Netherit darab &8&o(7%)",
                " &e‣ 10x Lazuritblokk &8&o(10%)",
                " &e‣ 10x Smaragdblokk &8&o(10%)",
                " &e‣ 10x Vasblokk &8&o(10%)",
                " &e‣ 3x Gyémántblokk &8&o(10%)",
                " &e‣ 1x Gyémántkard &5(Hatékonyság III, Szerencse III, Törhetetlenség III)",
                " &8&o(4%)",
                " &e‣ 1x Gyémántcsákány &5(Hatékonyság III, Szerencse III, Törhetetlenség III)",
                " &8&o(4%)",
                " &e‣ 1x Gyémántbalta &5(Hatékonyság III, Törhetetlenség III, Élesség III) &8&o(4%)",
                " &e‣ 1x Íj &5(Erő III, Törhetetlenség III) &8&o(4%)",
                " &e‣ 1x Törhetetlenség III könyv &8&o(7%)",
                " &e‣ 1x Szerencse III könyv &8&o(7%)",
                " &e‣ 1x Tüskék III könyv &8&o(7%)",
                " &e‣ 1x Halhatatlanság totem &8&o(5%)",
                " &e‣ 5x Végzet szeme &8&o(7%)"
        ));
        epicChest.put("material", "TRAPPED_CHEST");
        epicChest.put("cost", 100);
        epicChest.put("cooldown", 5);
        epicChest.put("buyCooldown", 300);
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
        Map<String, Object> legendaryChest = new LinkedHashMap<>();
        legendaryChest.put("name", "&6Legendás Láda");
        legendaryChest.put("description", Arrays.asList(
                "&eMegvásárolható láda.",
                "&e5 darab legendás tárgyat tartalmaz.",
                " ",
                "&bTartalmazhat:",
                " &e‣ 5x OP aranyalma &8&o(5%)",
                " &e‣ 15x Aranyalma &8&o(10%)",
                " &e‣ 3x Netherit rúd &8&o(6%)",
                " &e‣ 20x Smaragdblokk &8&o(10%)",
                " &e‣ 20x Vasblokk &8&o(10%)",
                " &e‣ 10x Gyémántblokk &8&o(10%)",
                " &e‣ 1x Kitinszárny &5(Törhetetlenség III) &8&o(5%)",
                " &e‣ 1x Netheritkard &5(Élesség V, Törhetetlenség III, Önjavítás,",
                "    &5Zsákmányolás III, Lángpallos II) &8&o(4%)",
                " &e‣ 1x Íj &5(Erő V, Törhetetlenség III, Végtelen, Láng, Ütés II) &8&o(4%)",
                " &e‣ 32x Tűzijáték &8&o(8%)",
                " &e‣ 1x Shulker doboz &8&o(5%)",
                " &e‣ 1x Önjavítás könyv &8&o(5%)",
                " &e‣ 1x Erő II itala &8&o(6%)",
                " &e‣ 3x Wither csontváz koponya &8&o(5%)",
                " &e‣ 15x Végzet szeme &8&o(7%)"
        ));
        legendaryChest.put("material", "ENDER_CHEST");
        legendaryChest.put("cost", 250);
        legendaryChest.put("cooldown", 5);
        legendaryChest.put("buyCooldown", 300);
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
                "common", commonChest,
                "uncommon", uncommonChest,
                "rare", rareChest,
                "epic", epicChest,
                "legendary", legendaryChest
        ));
    }
}
