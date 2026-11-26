package com.github.TavstalDev.rebus.util;

import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Utility class for time-related operations.
 */
public final class TimeUtil
{
    /**
     * Formats a duration (in seconds) into a localized string representation.
     * <br>
     * The formatted string includes days, hours, minutes, and seconds,
     * depending on the duration provided. Each time unit is localized
     * using the Rebus translation system.
     *
     * @param player The player for whom the localization is performed.
     * @param seconds The duration in seconds to be formatted.
     * @return A localized string representing the formatted duration.
     */
    public static String formatDuration(final ITranslationService translator, Player player, long seconds) {
        long days = seconds / 86400; // Calculate the number of days
        long hours = (seconds % 86400) / 3600; // Calculate the number of hours
        long minutes = (seconds % 3600) / 60; // Calculate the number of minutes
        long secs = seconds % 60; // Calculate the remaining seconds

        UUID playerId = player.getUniqueId();
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            // Append localized days string if days are greater than 0
            sb.append(translator.localize(playerId, "time.days",
                    MapUtil.of("%value%", String.valueOf(days)))).append(" ");
        }
        if (hours > 0 || days > 0) {
            // Append localized hours string if hours or days are greater than 0
            sb.append(translator.localize(playerId, "time.hours",
                    MapUtil.of("%value%", String.valueOf(hours)))).append(" ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            // Append localized minutes string if minutes, hours, or days are greater than 0
            sb.append(translator.localize(playerId, "time.minutes",
                    MapUtil.of("%value%", String.valueOf(minutes)))).append(" ");
        }
        if (secs > 0) {
            // Append localized seconds string if seconds are greater than 0
            sb.append(translator.localize(playerId, "time.seconds",
                    MapUtil.of("%value%", String.valueOf(secs)))).append(" ");
        }

        // Return the formatted string, trimmed of any trailing spaces
        return sb.toString().trim();
    }
}