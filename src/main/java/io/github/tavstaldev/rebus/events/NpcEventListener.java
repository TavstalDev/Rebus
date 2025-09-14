package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.RebusConfig;
import io.github.tavstaldev.rebus.gui.MainGUI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NpcEventListener implements Listener {
    private final RebusConfig config;

    public NpcEventListener() {
        this.config = Rebus.Config();
    }

    public static void init() {
        Rebus.Instance.getServer().getPluginManager().registerEvents(new NpcEventListener(), Rebus.Instance);
    }

    @EventHandler
    public void onNPCClick(NPCClickEvent event) {
        this.handleNPCInteraction(event.getNPC(), event.getClicker());
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        this.handleNPCInteraction(event.getNPC(), event.getClicker());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata("NPC")) {
            try {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc != null) {
                    this.handleNPCInteraction(npc, event.getPlayer());
                    event.setCancelled(true);
                }
            }
            catch (Exception e) {
                Rebus.Logger().Warn("Failed to identify NPC: " + e.getMessage());
            }
        }
    }

    private void handleNPCInteraction(NPC npc, Player player) {
        if (npc == null) {
            return;
        }
        if (!Rebus.Npcs().isRebusNPC(npc)) {
            return;
        }
        if (!player.hasPermission("rebus.use")) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }
        MainGUI.open(player);
    }
}