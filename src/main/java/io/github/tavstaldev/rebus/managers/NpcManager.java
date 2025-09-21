package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.models.NpcTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Manages the creation, removal, and identification of NPCs in the Rebus plugin.
 */
public class NpcManager {
    private final Rebus plugin;
    private final NPCRegistry registry;

    /**
     * Initializes the NpcManager with the Rebus plugin instance and NPC registry.
     */
    public NpcManager() {
        this.plugin = Rebus.Instance;
        this.registry = CitizensAPI.getNPCRegistry();
    }

    /**
     * Spawns an NPC at the player's current location with the configured name and skin.
     *
     * @param player The player at whose location the NPC will be spawned.
     */
    public void spawnNPC(Player player) {
        Location location = player.getLocation();
        RebusConfig config = Rebus.Config();

        String npcName = config.npcName;
        NPC npc = this.registry.createNPC(EntityType.PLAYER, npcName);
        npc.spawn(location);
        String skinData = config.npcSkin;
        if (!skinData.isEmpty()) {
            try {
                npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(npcName, "", skinData);
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Failed to apply skin to NPC: " + e.getMessage());
            }
        }
        npc.setProtected(true);
        try {
            npc.addTrait(new NpcTrait());
            Rebus.Logger().Info("Rebus NPC created for player: " + player.getName() + " (NPC ID: " + npc.getId() + ")");
        }
        catch (Exception e) {
            Rebus.Logger().Warn("Error setting NPC persistent data: " + e.getMessage());
        }
        plugin.sendLocalizedMsg(player, "NPC.Spawned");
    }

    /**
     * Removes all NPCs created by the Rebus plugin.
     */
    public void removeAllNpcs() {
        for (NPC npc : registry.sorted()) {
            if (isRebusNPC(npc)) {
                npc.destroy();
                Rebus.Logger().Info("Removed Rebus NPC (ID: " + npc.getId() + ")");
            }
        }
    }

    /**
     * Checks if the given NPC is a Rebus NPC by verifying if it has the NpcTrait.
     *
     * @param npc The NPC to check.
     * @return True if the NPC is a Rebus NPC, false otherwise.
     */
    public boolean isRebusNPC(NPC npc) {
        if (npc == null) {
            return false;
        }
        return npc.hasTrait(NpcTrait.class);
    }
}