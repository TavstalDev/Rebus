package io.github.tavstaldev.rebus.util;

import org.bukkit.Material;

/**
 * Utility class for handling icon-related operations, such as retrieving
 * materials by name or from configuration.
 */
public class IconUtils {

    /**
     * Retrieves a Material based on its name. If the name is null, empty, or
     * does not match any valid Material, a default Material (STONE) is returned.
     *
     * @param materialName The name of the material to retrieve.
     * @return The corresponding Material, or STONE if the name is invalid.
     */
    public static Material getMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.STONE; // Default material if name is null or empty
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE; // Default material if not found
        }
        return material;
    }
}