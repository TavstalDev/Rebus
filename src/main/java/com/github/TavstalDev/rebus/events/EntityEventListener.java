package com.github.TavstalDev.rebus.events;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Handles entity-related events to protect chests during the unlocking process.
 */
public class EntityEventListener implements Listener {
    private final IBoxService boxService;

    public EntityEventListener(Rebus plugin, IBoxService boxService) {
        this.boxService = boxService;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Prevents explosions from destroying blocks that are currently being unlocked.
     *
     * @param event The explosion event.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                boxService.occupiedLocations().contains(block.getLocation())
        );
    }
}
