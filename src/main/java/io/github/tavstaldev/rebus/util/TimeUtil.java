package io.github.tavstaldev.rebus.util;

import io.github.tavstaldev.rebus.Rebus;
import org.bukkit.entity.Player;

import java.util.Map;

public class TimeUtil
{
    public static String formatDuration(Player player, long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(Rebus.Translator().Localize(player, "Time.Days", Map.of("%value%", String.valueOf(days)))).append(" ");
        }
        if (hours > 0 || days > 0) {
            sb.append(Rebus.Translator().Localize(player, "Time.Hours", Map.of("%value%", String.valueOf(hours)))).append(" ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(Rebus.Translator().Localize(player, "Time.Minutes", Map.of("%value%", String.valueOf(minutes)))).append(" ");
        }
        if (secs > 0) {
            sb.append(Rebus.Translator().Localize(player, "Time.Seconds", Map.of("%value%", String.valueOf(secs)))).append(" ");
        }

        return sb.toString().trim();
    }
}
