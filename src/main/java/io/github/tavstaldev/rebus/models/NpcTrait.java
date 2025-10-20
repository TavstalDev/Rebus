package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
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
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        // Check if the clicked NPC is the one associated with this trait.
        if (event.getNPC() != this.getNPC()) {
            return;
        }
        var player = event.getClicker();

        // Check if the player has the required permission.
        if (!player.hasPermission("rebus.use")) {
            // Send a localized "no permission" message to the player.
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }
        // Open the main GUI for the player.
        MainGUI.open(player);
    }
}