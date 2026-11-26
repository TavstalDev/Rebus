package com.github.TavstalDev.rebus.models;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.gui.MainGUI;
import com.github.TavstalDev.yggraLib.bukkit.gui.IGuiService;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

/**
 * A custom NPC trait for the Rebus plugin.
 * This trait is applied to NPCs and handles right-click interactions.
 */
@TraitName("rebus")
public class NpcTrait extends Trait {
    /**
     * Constructor for the NpcTrait class.
     * Initializes the trait with the name "rebus".
     */
    public NpcTrait() {
        super("rebus");
    }

    /**
     * Event handler for NPC right-click events.
     * <p>
     * If the clicked NPC is not the one associated with this trait, the method exits early.
     * Otherwise, it checks if the player has the required permission to interact with the NPC.
     * If the player lacks permission, a localized "no permission" message is sent.
     * If the player has permission, the main GUI is opened for the player.
     * </p>
     *
     * @param event The NPCRightClickEvent triggered when a player right-clicks an NPC.
     */
    @EventHandler
    public void click(NPCRightClickEvent event) {
        // Check if the clicked NPC is the one associated with this trait.
        if (event.getNPC() != this.getNPC()) {
            return;
        }
        Player player = event.getClicker();

        // Check if the player has the required permission.
        if (!player.hasPermission("rebus.use")) {
            IChatService chatService = Rebus.instance.getService(IChatService.class);
            if (chatService != null)
                chatService.sendLocalizedMsg(player, "general.error.no-permission");
            return;
        }
        // Open the main GUI for the player.
        IGuiService manager = Rebus.instance.getService(IGuiService.class);
        if (manager == null)
            return;
        manager.open(player, MainGUI.ID);
    }
}