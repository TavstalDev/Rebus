package com.github.TavstalDev.rebus.commands;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.gui.MainGUI;
import com.github.TavstalDev.yggraLib.api.utils.ListUtil;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.commands.CommandBase;
import com.github.TavstalDev.yggraLib.bukkit.commands.SubCommand;
import com.github.TavstalDev.yggraLib.bukkit.gui.IGuiService;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommandRebus is the main command handler for the "rebus" command.
 * It implements the CommandExecutor interface to process commands
 * and their subcommands.
 */
public class CommandRebus extends CommandBase {
    private final Rebus plugin;
    private final IChatService chat;
    private final IGuiService gui;

    public CommandRebus(Rebus plugin) throws IllegalAccessException {
        super(plugin, "rebus", "rebus.commands.rebus", new ArrayList<SubCommand>() {
            {
                // HELP subcommand
                add(new SubCommand("help", "rebus.commands.rebus", MapUtil.of(
                        "syntax", "",
                        "description", "commands.help.desc"
                )));
                // VERSION subcommand
                add(new SubCommand("version", "rebus.commands.rebus.version", MapUtil.of(
                        "syntax", "",
                        "description", "commands.version.desc"
                )));
                // MENU subcommand
                add(new SubCommand("menu", "rebus.commands.rebus.menu", MapUtil.of(
                        "syntax", "",
                        "description", "commands.menu.desc"
                )));
            }
        });

        this.plugin = plugin;
        this.chat = plugin.getService(IChatService.class);
        this.gui = plugin.getService(IGuiService.class);
    }

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String @NotNull [] args) {
        // Handle console sender
        if (sender instanceof ConsoleCommandSender) {
            chat.sendCommandReply(sender, "commands.error.console-caller");
            return true;
        }

        // Handle player sender
        Player player = (Player) sender;
        if (!player.hasPermission("rebus.commands.rebus")) {
            chat.sendLocalizedMsg(player, "general.error.no-permission");
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
                            chat.sendLocalizedMsg(player, "commands.error.invalid-page");
                            return true;
                        }
                    }

                    sendHelp(player, page);
                    return true;
                }
                case "version": {
                    Map<String, Object> parameters = new HashMap<>();

                    parameters.put("version", plugin.version());
                    chat.sendLocalizedMsg(player, "commands.version.current", parameters);

                    Rebus.instance.isUpToDate().thenAccept(upToDate -> {
                        if (upToDate == null || upToDate) {
                            chat.sendLocalizedMsg(player, "commands.version.up-to-date");
                        } else {
                            chat.sendLocalizedMsg(player, "commands.version.outdated",
                                    MapUtil.of("link", plugin.downloadUrl()));
                        }
                    }).exceptionally(e -> {
                        chat.sendLocalizedMsg(player, "commands.version.exception");
                        return null;
                    });
                    return true;
                }
                case "menu": {
                    if (!player.hasPermission("rebus.commands.rebus.menu")) {
                        chat.sendLocalizedMsg(player, "general.no-permission");
                        return true;
                    }

                    if (gui == null)
                        return true;
                    gui.open(player, MainGUI.ID);
                    return true;
                }
            }

            // Invalid arguments
            chat.sendLocalizedMsg(player, "commands.error.invalid-arguments");
            return true;
        }

        // Default to help command
        sendHelp(player, 1);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        switch (args.length) {
            case 0:
            case 1: {
                return ListUtil.of("help", "version", "menu");
            }
            case 2: {
                String rawSubcommand = args[0].toLowerCase();
                if (rawSubcommand.equalsIgnoreCase("help") || rawSubcommand.equalsIgnoreCase("?"))
                    return ListUtil.of("1", "5", "10");
                return ListUtil.of();
            }
            default:
                return ListUtil.of();
        }
    }
}