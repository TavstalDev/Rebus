package io.github.tavstaldev.rebus.models;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.event.EventHandler;

@TraitName("rebus")
public class NpcTrait extends Trait {
    private Rebus plugin;

    public NpcTrait() {
        super("rebus");
        plugin = Rebus.Instance;
    }

    // An example event handler. All traits will be registered automatically as Spigot event Listeners
    @EventHandler
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        if (event.getNPC() != this.getNPC()) {
            return;
        }
        var player = event.getClicker();

        if (!player.hasPermission("rebus.use")) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return;
        }
        MainGUI.open(player);
    }
}