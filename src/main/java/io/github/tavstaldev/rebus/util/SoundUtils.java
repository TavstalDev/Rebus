package io.github.tavstaldev.rebus.util;

import com.cryptomorin.xseries.XSound;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Utility class for handling sound-related operations.
 */
public class SoundUtils {
    // Logger instance for logging debug information related to SoundUtils.
    private static final PluginLogger _logger = Rebus.Logger().WithModule(SoundUtils.class);

    /**
     * Retrieves an XSound instance based on the provided sound name.
     * <br>
     * This method attempts to resolve a sound name to an XSound instance.
     * If the name is "none" (case-insensitive), it returns an empty Optional.
     * If an exception occurs during the resolution, it logs the error and
     * returns an empty Optional.
     *
     * @param name The name of the sound to resolve. Must not be null.
     * @return An Optional containing the resolved XSound, or an empty Optional if
     *         the sound name is "none" or if an error occurs.
     */
    public static Optional<XSound> getSound(@NotNull String name) {
        try {
            String key = name.toLowerCase(Locale.ROOT);
            // Fixes null pointer exception
            if ("none".equalsIgnoreCase(key))
                return Optional.empty();

            return XSound.of(key);
        }
        catch (Exception ex) {
            _logger.Debug("Failed to get sound for name: " + name);
            _logger.Debug("Exception: " + ex.getMessage());
            return Optional.empty();
        }
    }
}
