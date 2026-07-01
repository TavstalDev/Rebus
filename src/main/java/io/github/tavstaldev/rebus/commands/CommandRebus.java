package io.github.tavstaldev.rebus.commands;

import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.gui.MainGUI;
import io.github.tavstaldev.yggra.core.commands.CommandBase;
import io.github.tavstaldev.yggra.core.commands.SubCommand;
import io.github.tavstaldev.yggra.core.gui.GuiManager;
import io.github.tavstaldev.yggra.core.services.ChatService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CommandRebus is the main command handler for the "rebus" command.
 * It implements the CommandExecutor interface to process commands
 * and their subcommands.
 */
public class CommandRebus extends CommandBase {
    private final Rebus plugin;
    private final ChatService chat;
    private final GuiManager gui;

    public CommandRebus(Rebus plugin) throws IllegalAccessException {
        super(plugin, "rebus", "rebus.commands.rebus", new ArrayList<>() {
            {
                // HELP subcommand
                add(new SubCommand("help", "rebus.commands.rebus", Map.of(
                        "syntax", "",
                        "description", "commands.help.desc"
                )));
                // VERSION subcommand
                add(new SubCommand("version", "rebus.commands.rebus.version", Map.of(
                        "syntax", "",
                        "description", "commands.version.desc"
                )));
                // MENU subcommand
                add(new SubCommand("menu", "rebus.commands.rebus.menu", Map.of(
                        "syntax", "",
                        "description", "commands.menu.desc"
                )));
            }
        });

        this.plugin = plugin;
        this.chat = plugin.chat();
        this.gui = plugin.gui();
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
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
                    chat.sendLocalizedMsg(player, "commands.version.header");
                    //noinspection UnstableApiUsage
                    chat.sendLocalizedMsg(player, "commands.version.current", Map.of("version", plugin.getPluginMeta().getVersion()));
                    chat.sendLocalizedMsg(player, "commands.version.bottom");

                    /*chat.isUpToDate().thenAccept(upToDate -> {
                        if (upToDate) {
                            chat.sendLocalizedMsg(player, "Commands.Version.UpToDate");
                        } else {
                            chat.sendLocalizedMsg(player, "Commands.Version.Outdated", Map.of("link", chat.getDownloadUrl()));
                        }
                    }).exceptionally(e -> {
                        logger.error("Failed to determine update status.", e);
                        return null;
                    });*/
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        switch (args.length) {
            case 0:
            case 1: {
                return List.of("help", "version", "menu");
            }
            case 2: {
                String rawSubcommand = args[0].toLowerCase();
                if (rawSubcommand.equalsIgnoreCase("help") || rawSubcommand.equalsIgnoreCase("?"))
                    return List.of("1", "5", "10");
                return List.of();
            }
            default:
                return List.of();
        }
    }
}