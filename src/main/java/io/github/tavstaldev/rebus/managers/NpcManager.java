package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcManager {
    private final Rebus plugin;
    private final Map<UUID, NPC> playerNPCs = new HashMap<>();
    private final Map<Integer, UUID> npcToPlayerMap = new HashMap<>();
    private final NPCRegistry registry;
    private final File npcDataFile;
    private FileConfiguration npcDataConfig;
    private static final String REBUS_NPC_KEY = "rebus.is_rebus_npc";
    private static final String REBUS_OWNER_KEY = "rebus.owner";

    public NpcManager() {
        this.plugin = Rebus.Instance;
        this.registry = CitizensAPI.getNPCRegistry();
        this.npcDataFile = new File(plugin.getDataFolder(), "npcs.yml");
        this.loadNPCData();
    }

    private void loadNPCData() {
        if (!this.npcDataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                this.npcDataFile.createNewFile();
            }
            catch (IOException e) {
                Rebus.Logger().Error("Failed to create npcs.yml: " + e.getMessage());
                return;
            }
        }
        this.npcDataConfig = YamlConfiguration.loadConfiguration((File)this.npcDataFile);
    }

    private void saveNPCData() {
        try {
            this.npcDataConfig.save(this.npcDataFile);
        }
        catch (IOException e) {
            Rebus.Logger().Error("Failed to save npcs.yml: " + e.getMessage());
        }
    }

    public void loadExistingNPCs() {
        Rebus.Logger().Info("Loading Rebus NPCs...");
        for (NPC npc : this.registry) {
            if (!this.isRebusNPC(npc))
                continue;
            try {
                String ownerUUID = (String)npc.data().get(REBUS_OWNER_KEY, (Object)"");
                if (ownerUUID.isEmpty())
                    continue;
                UUID playerUUID = UUID.fromString(ownerUUID);
                this.playerNPCs.put(playerUUID, npc);
                this.npcToPlayerMap.put(npc.getId(), playerUUID);
                this.npcDataConfig.set("npcs." + npc.getId() + ".owner", ownerUUID);
                this.npcDataConfig.set("npcs." + npc.getId() + ".is_rebus", true);
            }
            catch (Exception e) {
                Rebus.Logger().Error("Error loading NPC (ID: " + npc.getId() + "): " + e.getMessage());
                return;
            }
        }
        this.saveNPCData();
        Rebus.Logger().Info(this.playerNPCs.size() + " Rebus NPCs loaded.");
    }

    public void spawnNPC(Player player) {
        Location location = player.getLocation();
        if (this.playerNPCs.containsKey(player.getUniqueId())) {
            this.removeNPC(player);
        }
        RebusConfig config = Rebus.Config();

        String npcName = config.npcName;
        NPC npc = this.registry.createNPC(EntityType.PLAYER, npcName);
        npc.spawn(location);
        String skinData = config.npcSkin;
        if (!skinData.isEmpty()) {
            try {
                SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                skinTrait.setSkinPersistent(npcName, "", skinData);
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Failed to apply skin to NPC: " + e.getMessage());
            }
        }
        npc.setProtected(true);
        try {
            npc.data().setPersistent(REBUS_NPC_KEY, true);
            npc.data().setPersistent(REBUS_OWNER_KEY, (Object)player.getUniqueId().toString());
            this.playerNPCs.put(player.getUniqueId(), npc);
            this.npcToPlayerMap.put(npc.getId(), player.getUniqueId());
            this.npcDataConfig.set("npcs." + npc.getId() + ".owner", (Object)player.getUniqueId().toString());
            this.npcDataConfig.set("npcs." + npc.getId() + ".is_rebus", true);
            this.saveNPCData();
            Rebus.Logger().Info("Rebus NPC created for player: " + player.getName() + " (NPC ID: " + npc.getId() + ")");
        }
        catch (Exception e) {
            Rebus.Logger().Warn("Error setting NPC persistent data: " + e.getMessage());
        }
        plugin.sendLocalizedMsg(player, "NPC.Spawned");
    }

    public void removeNPC(Player player) {
        NPC npc;
        UUID uuid = player.getUniqueId();
        if (this.playerNPCs.containsKey(uuid) && (npc = this.playerNPCs.get(uuid)) != null) {
            try {
                this.npcToPlayerMap.remove(npc.getId());
                this.npcDataConfig.set("npcs." + npc.getId(), null);
                this.saveNPCData();
                npc.destroy();
                this.playerNPCs.remove(uuid);
                Rebus.Logger().Info("Rebus NPC removed: " + player.getName() + " (NPC ID: " + npc.getId() + ")");
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Error removing NPC: " + e.getMessage());
            }
            plugin.sendLocalizedMsg(player, "NPC.Removed");
        }
    }

    public boolean isRebusNPC(NPC npc) {
        if (npc == null) {
            return false;
        }
        try {
            if (npc.data().has(REBUS_NPC_KEY)) {
                return (Boolean)npc.data().get(REBUS_NPC_KEY, (Object)false);
            }
            if (npc.data().has("rebus.npc")) {
                npc.data().setPersistent(REBUS_NPC_KEY, true);
                if (npc.data().has(REBUS_OWNER_KEY)) {
                    npc.data().setPersistent(REBUS_OWNER_KEY, npc.data().get(REBUS_OWNER_KEY, (Object)""));
                }
                return true;
            }
        }
        catch (Exception e) {
            Rebus.Logger().Warn("Error checking NPC persistent data: " + e.getMessage());
        }
        if (this.npcDataConfig.getBoolean("npcs." + npc.getId() + ".is_rebus", false)) {
            try {
                npc.data().setPersistent(REBUS_NPC_KEY, true);
                String owner = this.npcDataConfig.getString("npcs." + npc.getId() + ".owner", "");
                if (!owner.isEmpty()) {
                    npc.data().setPersistent(REBUS_OWNER_KEY, owner);
                    UUID playerUUID = UUID.fromString(owner);
                    this.playerNPCs.put(playerUUID, npc);
                    this.npcToPlayerMap.put(npc.getId(), playerUUID);
                }
                return true;
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Error restoring file-based NPC data: " + e.getMessage());
            }
        }
        RebusConfig config = Rebus.Config();
        String expectedName = config.npcName;
        if (npc.getName().equals(expectedName)) {
            Rebus.Logger().Info("NPC recognized by name, updating persistent data: " + npc.getName() + " (ID: " + npc.getId() + ")");
            try {
                npc.data().setPersistent(REBUS_NPC_KEY, true);
                return true;
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Error setting name-based NPC persistent data: " + e.getMessage());
            }
        }
        if (this.npcToPlayerMap.containsKey(npc.getId())) {
            return true;
        }
        for (NPC playerNPC : this.playerNPCs.values()) {
            if (playerNPC == null || playerNPC.getId() != npc.getId()) continue;
            return true;
        }
        return false;
    }

    public void removeAllNPCs() {
        ArrayList<NPC> toRemove = new ArrayList<NPC>();
        for (NPC npc : this.registry) {
            if (!this.isRebusNPC(npc)) continue;
            toRemove.add(npc);
        }
        for (NPC npc : toRemove) {
            try {
                this.npcToPlayerMap.remove(npc.getId());
                this.npcDataConfig.set("npcs." + npc.getId(), null);
                npc.destroy();
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Error removing NPC (ID: " + npc.getId() + "): " + e.getMessage());
            }
        }
        this.playerNPCs.clear();
        this.npcToPlayerMap.clear();
        this.saveNPCData();
        Rebus.Logger().Info("Removed all Rebus NPCs.");
    }

    public void shutdown() {
        for (Map.Entry<UUID, NPC> entry : this.playerNPCs.entrySet()) {
            try {
                NPC npc = entry.getValue();
                this.npcDataConfig.set("npcs." + npc.getId() + ".owner", entry.getKey().toString());
                this.npcDataConfig.set("npcs." + npc.getId() + ".is_rebus", true);
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Failed to save NPC data on shutdown: " + e.getMessage());
            }
        }
        this.saveNPCData();
    }
}
