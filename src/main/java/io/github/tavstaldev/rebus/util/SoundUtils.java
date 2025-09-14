package io.github.tavstaldev.rebus.util;

import com.cryptomorin.xseries.XSound;
import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.rebus.Rebus;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public class SoundUtils {
    private static final PluginLogger _logger = Rebus.Logger().WithModule(SoundUtils.class);

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