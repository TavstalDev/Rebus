package io.github.tavstaldev.rebus.util;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for handling permissions using the Vault API.
 */
public class PermissionUtils
{
    // Instance of the Vault Permission API
    private static Permission perms = null;

    /**
     * Sets up the permissions system by hooking into Vault.
     *
     * @return true if the permissions system was successfully set up, false otherwise.
     */
    public static boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        perms = rsp.getProvider();
        return true;
    }

    /**
     * Checks if the permissions system supports groups.
     *
     * @return true if group support is available, false otherwise.
     */
    public static boolean hasGroupSupport() {
        if (perms != null) {
            return perms.hasGroupSupport();
        }
        return false;
    }

    /**
     * Checks if an offline player has a specific permission.
     *
     * @param player The offline player to check.
     * @param permission The permission to check for.
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean checkOfflinePermission(OfflinePlayer player, String permission) {
        if (perms != null) {
            return perms.playerHas(null, player, permission);
        }
        return false;
    }

    /**
     * Checks if an online player has a specific permission.
     *
     * @param player The online player to check.
     * @param permission The permission to check for.
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean checkPermission(Player player, String permission) {
        if (perms != null) {
            return perms.playerHas(player, permission);
        }
        return false;
    }

    /**
     * Gets the primary group of an offline player.
     *
     * @param player The offline player to check.
     * @return The name of the primary group, or null if group support is not available or the player has no group.
     */
    public static @Nullable String getPrimaryGroup(OfflinePlayer player) {
        if (perms != null) {
            if (!perms.hasGroupSupport())
                return null;
            return perms.getPrimaryGroup(null, player);
        }
        return null;
    }

    /**
     * Gets the primary group of an online player.
     *
     * @param player The online player to check.
     * @return The name of the primary group, or null if group support is not available or the player has no group.
     */
    public static @Nullable String getPrimaryGroup(Player player) {
        if (perms != null) {
            if (!perms.hasGroupSupport())
                return null;
            return perms.getPrimaryGroup(player);
        }
        return null;
    }
}
