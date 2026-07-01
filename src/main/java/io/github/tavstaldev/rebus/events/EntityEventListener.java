package io.github.tavstaldev.rebus.events;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.managers.ChestManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Handles entity-related events to protect chests during the unlocking process.
 */
public class EntityEventListener implements Listener {
    private final ChestManager chestManager;

    /**
     * Creates the entity event listener and registers it with the server.
     *
     * @param plugin       The plugin instance.
     * @param chestManager The chest manager for checking unlocking states.
     */
    public EntityEventListener(Rebus plugin, ChestManager chestManager) {
        this.chestManager = chestManager;
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
                chestManager.chestsUnderUnlocking.contains(block.getLocation())
        );
    }
}
