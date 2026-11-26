package com.github.TavstalDev.rebus.services.npc;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

public interface INpcService {
    void spawnNPC(Player player);

    void removeAllNPCs();

    boolean isRebusNPC(NPC npc);
}
