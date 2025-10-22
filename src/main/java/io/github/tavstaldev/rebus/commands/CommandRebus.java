package io.github.tavstaldev.rebus.commands;

import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.models.command.SubCommandData;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommandRebus is the main command handler for the "rebus" command.
 * It implements the CommandExecutor interface to process commands
 * and their subcommands.
 */
public class CommandRebus implements CommandExecutor {
    // Logger instance for logging messages related to this command.
    private final PluginLogger _logger = Rebus.logger().withModule(CommandRebus.class);

    // List of subcommands available for the "rebus" command.
    private final List<SubCommandData> _subCommands = new ArrayList<>() {
        {
            // HELP subcommand
            add(new SubCommandData("help", "", Map.of(
                    "syntax", "",
                    "description", "Commands.Help.Desc"
            )));
            // VERSION subcommand
            add(new SubCommandData("version", "", Map.of(
                    "syntax", "",
                    "description", "Commands.Version.Desc"
            )));
            // MENU subcommand
            add(new SubCommandData("menu", "rebus.use", Map.of(
                    "syntax", "",
                    "description", "Commands.Menu.Desc"
            )));
        }
    };

    /**
     * Handles the execution of the "rebus" command.
     *
     * @param sender  The sender of the command (e.g., player or console).
     * @param command The command being executed.
     * @param label   The alias of the command used.
     * @param args    The arguments provided with the command.
     * @return true if the command was successfully executed, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        // Handle console sender
        if (sender instanceof ConsoleCommandSender) {
            _logger.info(ChatUtils.translateColors("Commands.ConsoleCaller", true).toString());
            return true;
        }

        // Handle player sender
        Player player = (Player) sender;
        if (!player.hasPermission("rebus.use")) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return true;
        }

        // Process subcommands
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "help":
                case "?": {
                    int page = 1;
                    if (args.length > 1) {
                        try {
                            page = Integer.parseInt(args[1]);
                        } catch (Exception ex) {
                            Rebus.Instance.sendLocalizedMsg(player, "Commands.Common.InvalidPage");
                            return true;
                        }
                    }

                    help(player, page);
                    return true;
                }
                case "version": {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("version", Rebus.Instance.getVersion());
                    Rebus.Instance.sendLocalizedMsg(player, "Commands.Version.Current", parameters);

                    Rebus.Instance.isUpToDate().thenAccept(upToDate -> {
                        if (upToDate) {
                            Rebus.Instance.sendLocalizedMsg(player, "Commands.Version.UpToDate");
                        } else {
                            Rebus.Instance.sendLocalizedMsg(player, "Commands.Version.Outdated", Map.of("link", Rebus.Instance.getDownloadUrl()));
                        }
                    }).exceptionally(e -> {
                        _logger.error("Failed to determine update status: " + e.getMessage());
                        return null;
                    });
                    return true;
                }
                case "menu": {
                    if (!player.hasPermission("rebus.use")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }

                    MainGUI.open(player);
                    return true;
                }
            }

            // Invalid arguments
            Rebus.Instance.sendLocalizedMsg(player, "Commands.InvalidArguments");
            return true;
        }

        // Default to help command
        help(player, 1);
        return true;
    }

    /**
     * Displays the help menu for the "rebus" command.
     *
     * @param player The player requesting help.
     * @param page   The page number of the help menu to display.
     */
    private void help(Player player, int page) {
        int maxPage = 1 + (_subCommands.size() / 15);

        // Ensure the page number is within valid bounds
        if (page > maxPage)
            page = maxPage;
        if (page < 1)
            page = 1;
        int finalPage = page;

        // Send help title and info
        Rebus.Instance.sendLocalizedMsg(player, "Commands.Help.Title", Map.of(
                        "current_page", finalPage,
                        "max_page", maxPage
                )
        );
        Rebus.Instance.sendLocalizedMsg(player, "Commands.Help.Info");

        // Display subcommands
        boolean reachedEnd = false;
        int itemIndex = 0;
        for (int i = 0; i < 15; i++) {
            int index = itemIndex + (page - 1) * 15;
            if (index >= _subCommands.size()) {
                reachedEnd = true;
                break;
            }
            itemIndex++;

            SubCommandData subCommand = _subCommands.get(index);
            if (!subCommand.hasPermission(player)) {
                i--;
                continue;
            }

            subCommand.send(Rebus.Instance, player, "rebus");
        }

        // Display navigation buttons
        String previousBtn = Rebus.Instance.localize(player, "Commands.Help.PrevBtn");
        String nextBtn = Rebus.Instance.localize(player, "Commands.Help.NextBtn");
        String bottomMsg = Rebus.Instance.localize(player, "Commands.Help.Bottom")
                .replace("%current_page%", String.valueOf(page))
                .replace("%max_page%", String.valueOf(maxPage));

        Map<String, Component> bottomParams = new HashMap<>();
        if (page > 1)
            bottomParams.put("previous_btn", ChatUtils.translateColors(previousBtn, true).clickEvent(ClickEvent.runCommand("/rebus help " + (page - 1))));
        else
            bottomParams.put("previous_btn", ChatUtils.translateColors(previousBtn, true));

        if (!reachedEnd && maxPage >= page + 1)
            bottomParams.put("next_btn", ChatUtils.translateColors(nextBtn, true).clickEvent(ClickEvent.runCommand("/rebus help " + (page + 1))));
        else
            bottomParams.put("next_btn", ChatUtils.translateColors(nextBtn, true));

        Component bottomComp = ChatUtils.buildWithButtons(bottomMsg, bottomParams);
        player.sendMessage(bottomComp);
    }
}