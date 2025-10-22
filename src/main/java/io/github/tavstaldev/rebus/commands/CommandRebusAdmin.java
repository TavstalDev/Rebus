package io.github.tavstaldev.rebus.commands;

import io.github.tavstaldev.minecorelib.core.PluginLogger;
import io.github.tavstaldev.minecorelib.models.command.SubCommandData;
import io.github.tavstaldev.minecorelib.utils.ChatUtils;
import io.github.tavstaldev.rebus.Rebus;
import io.github.tavstaldev.rebus.models.RebusChest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * CommandRebusAdmin is the command handler for the "rebusadmin" command.
 * It implements the CommandExecutor interface to process admin-level commands
 * and their subcommands.
 */
public class CommandRebusAdmin implements CommandExecutor {
    // Logger instance for logging messages related to this command.
    private final PluginLogger _logger = Rebus.logger().withModule(CommandRebusAdmin.class);

    // List of subcommands available for the "rebusadmin" command.
    private final List<SubCommandData> _subCommands = new ArrayList<>() {
        {
            // HELP subcommand
            add(new SubCommandData("help", "rebus.admin", Map.of(
                    "syntax", "",
                    "description", "Commands.Help.Desc"
            )));
            // VERSION subcommand
            add(new SubCommandData("version", "rebus.info", Map.of(
                    "syntax", "",
                    "description", "Commands.Version.Desc"
            )));
            // RELOAD subcommand
            add(new SubCommandData("reload", "rebus.reload", Map.of(
                    "syntax", "",
                    "description", "Commands.Reload.Desc"
            )));
            // NPC subcommand
            add(new SubCommandData("npc", "rebus.npc", Map.of(
                    "syntax", "",
                    "description", "Commands.Npc.Desc"
            )));
            // GIVE subcommand
            add(new SubCommandData("give", "rebus.give", Map.of(
                    "syntax", "Commands.Give.Syntax",
                    "description", "Commands.Give.Desc"
            )));
        }
    };

    /**
     * Handles the execution of the "rebusadmin" command.
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
        if (!player.hasPermission("rebus.admin")) {
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
                case "reload": {
                    if (!player.hasPermission("rebus.reload")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }

                    Rebus.Instance.reload();
                    Rebus.Instance.sendLocalizedMsg(player, "Commands.Reload.Done");
                    return true;
                }
                case "npc": {
                    if (!player.hasPermission("rebus.npc")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }

                    Rebus.npcManager().spawnNPC(player);
                    return true;
                }
                case "removenpcs": {
                    if (!player.hasPermission("rebus.npc")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }
                    Rebus.npcManager().removeAllNpcs();
                    return true;
                }
                case "give": {
                    if (!player.hasPermission("rebus.give")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }

                    if (args.length != 3) {
                        Rebus.Instance.sendLocalizedMsg(player, "Commands.InvalidArguments");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.PlayerNotFound");
                        return true;
                    }

                    RebusChest chest = null;
                    for (RebusChest c : Rebus.chestManager().getChests()) {
                        if (Objects.equals(c.getKey(), args[2])) {
                            chest = c;
                            break;
                        }
                    }

                    if (chest == null) {
                        Rebus.Instance.sendLocalizedMsg(player, "Chests.NotFound", Map.of("chest", args[2]));
                        return true;
                    }

                    chest.give(target, 1);
                    Rebus.Instance.sendLocalizedMsg(player, "Commands.Give.Given", Map.of(
                            "chest", chest.getName(),
                            "player", target.getName()
                    ));
                    Rebus.Instance.sendLocalizedMsg(target, "Commands.Give.Received", Map.of(
                            "chest", chest.getName(),
                            "player", player.getName()
                    ));
                    return true;
                }
                case "reset": {
                    if (!player.hasPermission("rebus.reset")) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
                        return true;
                    }

                    if (args.length != 2) {
                        Rebus.Instance.sendLocalizedMsg(player, "Commands.InvalidArguments");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        Rebus.Instance.sendLocalizedMsg(player, "General.PlayerNotFound");
                        return true;
                    }

                    Rebus.database().removeAllCooldowns(target.getUniqueId());
                    Rebus.Instance.sendLocalizedMsg(player, "General.ResetCooldowns", Map.of("player", target.getName()));
                    Rebus.Instance.sendLocalizedMsg(target, "General.YourCooldownsReset");
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
     * Displays the help menu for the "rebusadmin" command.
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

            subCommand.send(Rebus.Instance, player, "rebusadmin");
        }

        // Display navigation buttons
        String previousBtn = Rebus.Instance.localize(player, "Commands.Help.PrevBtn");
        String nextBtn = Rebus.Instance.localize(player, "Commands.Help.NextBtn");
        String bottomMsg = Rebus.Instance.localize(player, "Commands.Help.Bottom")
                .replace("%current_page%", String.valueOf(page))
                .replace("%max_page%", String.valueOf(maxPage));

        Map<String, Component> bottomParams = new HashMap<>();
        if (page > 1)
            bottomParams.put("previous_btn", ChatUtils.translateColors(previousBtn, true).clickEvent(ClickEvent.runCommand("/rebusadmin help " + (page - 1))));
        else
            bottomParams.put("previous_btn", ChatUtils.translateColors(previousBtn, true));

        if (!reachedEnd && maxPage >= page + 1)
            bottomParams.put("next_btn", ChatUtils.translateColors(nextBtn, true).clickEvent(ClickEvent.runCommand("/rebusadmin help " + (page + 1))));
        else
            bottomParams.put("next_btn", ChatUtils.translateColors(nextBtn, true));

        Component bottomComp = ChatUtils.buildWithButtons(bottomMsg, bottomParams);
        player.sendMessage(bottomComp);
    }
}