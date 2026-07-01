package io.github.tavstaldev.rebus.managers;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.models.NpcTrait;
import io.github.tavstaldev.yggra.core.logger.YggraLogger;
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
    private final YggraLogger logger;
    private final Rebus plugin;
    private final NPCRegistry registry;

    /**
     * Initializes the NpcManager with the Rebus plugin instance and NPC registry.
     */
    public NpcManager(Rebus plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger().withModule(NpcManager.class);
        this.registry = CitizensAPI.getNPCRegistry();
    }

    /**
     * Spawns an NPC at the player's current location with the configured name and skin.
     *
     * @param player The player at whose location the NPC will be spawned.
     */
    public void spawnNPC(Player player) {
        Location location = player.getLocation();
        RebusConfig config = plugin.config();

        String npcName = config.npcName;
        NPC npc = this.registry.createNPC(EntityType.PLAYER, npcName);
        String skinData = config.npcSkin;
        if (!skinData.isEmpty()) {
            try {
                npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(npcName, config.npcSignature, skinData);
            }
            catch (Exception e) {
                logger.warn("Failed to apply skin to NPC.", e);
            }
        }
        npc.spawn(location);
        npc.setProtected(true);
        try {
            var trait = new NpcTrait();
            npc.addTrait(trait);
            logger.info("Rebus NPC created for player: " + player.getName() + " (NPC ID: " + npc.getId() + ")");
        }
        catch (Exception e) {
            logger.warn("Error setting NPC persistent data.", e);
        }
        plugin.chat().sendLocalizedMsg(player, "npc.spawned");
    }

    /**
     * Removes all NPCs created by the Rebus plugin.
     */
    public void removeAllNPCs() {
        for (NPC npc : registry.sorted()) {
            if (isRebusNPC(npc)) {
                npc.destroy();
                logger.info("Removed Rebus NPC (ID: " + npc.getId() + ")");
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
        if (npc == null)
            return false;
        return npc.hasTrait(NpcTrait.class);
    }
}