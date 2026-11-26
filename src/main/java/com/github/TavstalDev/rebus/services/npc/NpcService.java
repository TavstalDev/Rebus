package com.github.TavstalDev.rebus.services.npc;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.config.RebusConfig;
import com.github.TavstalDev.rebus.models.NpcTrait;
import com.github.TavstalDev.yggraLib.api.logger.YggraLogger;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/** {@inheritDoc} */
public class NpcService implements INpcService {
    private final YggraLogger logger;
    private final Rebus plugin;
    private final NPCRegistry registry;
    private final IChatService chat;

    public NpcService(Rebus plugin, IChatService chatService) {
        this.plugin = plugin;
        this.logger = plugin.logger().withModule(NpcService.class);
        this.registry = CitizensAPI.getNPCRegistry();
        this.chat = chatService;
    }

    /** {@inheritDoc} */
    public void spawnNPC(Player player) {
        Location location = player.getLocation();
        RebusConfig config = plugin.config();

        String npcName = config.npc.getName();
        NPC npc = this.registry.createNPC(EntityType.PLAYER, npcName);
        String skinData = config.npc.getSkin();
        if (!skinData.isEmpty()) {
            try {
                npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(npcName, config.npc.getSignature(), skinData);
            }
            catch (Exception e) {
                logger.warn("Failed to apply skin to NPC.", e);
            }
        }
        npc.spawn(location);
        npc.setProtected(true);
        try {
            NpcTrait trait = new NpcTrait();
            npc.addTrait(trait);
            logger.info("Rebus NPC created for player: " + player.getName() + " (NPC ID: " + npc.getId() + ")");
        }
        catch (Exception e) {
            logger.warn("Error setting NPC persistent data.", e);
        }
        chat.sendLocalizedMsg(player, "npc.spawned");
    }

    /** {@inheritDoc} */
    public void removeAllNPCs() {
        for (NPC npc : registry.sorted()) {
            if (isRebusNPC(npc)) {
                npc.destroy();
                logger.info("Removed Rebus NPC (ID: " + npc.getId() + ")");
            }
        }
    }

    /** {@inheritDoc} */
    public boolean isRebusNPC(NPC npc) {
        if (npc == null)
            return false;
        return npc.hasTrait(NpcTrait.class);
    }
}
