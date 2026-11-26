package com.github.TavstalDev.rebus.commands;

import com.github.TavstalDev.rebus.Rebus;
import com.github.TavstalDev.rebus.database.IRebusDatabase;
import com.github.TavstalDev.rebus.models.Chest;
import com.github.TavstalDev.rebus.services.box.IBoxService;
import com.github.TavstalDev.rebus.services.npc.INpcService;
import com.github.TavstalDev.yggraLib.api.database.QueryCondition;
import com.github.TavstalDev.yggraLib.api.localization.ITranslationService;
import com.github.TavstalDev.yggraLib.api.scheduler.IScheduler;
import com.github.TavstalDev.yggraLib.api.scheduler.YggraTask;
import com.github.TavstalDev.yggraLib.api.utils.ListUtil;
import com.github.TavstalDev.yggraLib.api.utils.MapUtil;
import com.github.TavstalDev.yggraLib.bukkit.commands.CommandBase;
import com.github.TavstalDev.yggraLib.bukkit.commands.SubCommand;
import com.github.TavstalDev.yggraLib.bukkit.services.IChatService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CommandRebusAdmin is the command handler for the "rebusadmin" command.
 * It implements the CommandExecutor interface to process admin-level commands
 * and their subcommands.
 */
public class CommandRebusAdmin extends CommandBase {
    private final Rebus plugin;
    private final IChatService chat;
    private final ITranslationService translator;
    private final IBoxService chestManager;
    private final INpcService npcManager;
    private final IRebusDatabase database;
    private final IScheduler scheduler;

    public CommandRebusAdmin(Rebus plugin) throws IllegalAccessException {
        super(plugin, "rebusadmin", "rebus.commands.rebusadmin", new ArrayList<SubCommand>() {
            {
                // HELP subcommand
                add(new SubCommand("help", "rebus.commands.rebusadmin", MapUtil.of(
                        "syntax", "",
                        "description", "commands.help.desc"
                )));
                // RELOAD subcommand
                add(new SubCommand("reload", "rebus.commands.rebusadmin.reload", MapUtil.of(
                        "syntax", "",
                        "description", "commands.reload.desc"
                )));
                // NPC subcommand
                add(new SubCommand("npc", "rebus.commands.rebusadmin.npc", MapUtil.of(
                        "syntax", "",
                        "description", "commands.npc.desc"
                )));
                // GIVE subcommand
                add(new SubCommand("give", "rebus.commands.rebusadmin.give", MapUtil.of(
                        "syntax", "commands.give.syntax",
                        "description", "commands.give.desc"
                )));
                // RESET subcommand
                add(new SubCommand("reset", "rebus.commands.rebusadmin.reset", MapUtil.of(
                        "syntax", "commands.reset.syntax",
                        "description", "commands.reset.desc"
                )));
            }
        });

        this.plugin = plugin;
        this.chat = plugin.getService(IChatService.class);
        this.translator = plugin.getService(ITranslationService.class);
        this.chestManager = plugin.getService(IBoxService.class);
        this.npcManager = plugin.getService(INpcService.class);
        this.database = plugin.getService(IRebusDatabase.class);
        this.scheduler = plugin.getService(IScheduler.class);
    }

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String @NotNull [] args) {
        // Handle console sender
        if (sender instanceof ConsoleCommandSender) {
            chat.sendCommandReply(sender, "commands.error.console-caller");
            return true;
        }

        // Handle player sender
        Player player = (Player) sender;
        if (!player.hasPermission("rebus.commands.rebusadmin")) {
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
                case "reload": {
                    if (!player.hasPermission("rebus.commands.rebusadmin.reload")) {
                        chat.sendLocalizedMsg(player, "general.error.no-permission");
                        return true;
                    }

                    plugin.reload();
                    chat.sendLocalizedMsg(player, "commands.reload.done");
                    return true;
                }
                case "npc": {
                    if (!player.hasPermission("rebus.commands.rebusadmin.npc")) {
                        chat.sendLocalizedMsg(player, "general.error.no-permission");
                        return true;
                    }

                    npcManager.spawnNPC(player);
                    return true;
                }
                case "remove-npcs":
                case "removenpcs": {
                    if (!player.hasPermission("rebus.commands.rebusadmin.npc")) {
                        chat.sendLocalizedMsg(player, "general.error.no-permission");
                        return true;
                    }
                   npcManager.removeAllNPCs();
                    return true;
                }
                case "give": {
                    if (!player.hasPermission("rebus.commands.rebusadmin.give")) {
                        chat.sendLocalizedMsg(player, "general.error.no-permission");
                        return true;
                    }

                    if (args.length != 3) {
                        chat.sendLocalizedMsg(player, "commands.error.invalid-arguments");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        chat.sendLocalizedMsg(player, "general.error.player-not-found");
                        return true;
                    }

                    Chest chest = chestManager.getChest(args[2]);
                    if (chest == null) {
                        chat.sendLocalizedMsg(player, "chests.error.not-found", MapUtil.of("chest", args[2]));
                        return true;
                    }

                    chestManager.giveChest(target, chest, 1);
                    chat.sendLocalizedMsg(player, "commands.give.given", MapUtil.of(
                            "chest", translator.localize(player.getUniqueId(), chest.getNameKey()),
                            "player", target.getName()
                    ));
                    chat.sendLocalizedMsg(target, "commands.give.received", MapUtil.of(
                            "chest", translator.localize(target.getUniqueId(), chest.getNameKey()),
                            "player", player.getName()
                    ));
                    return true;
                }
                case "reset": {
                    if (!player.hasPermission("rebus.commands.rebusadmin.reset")) {
                        chat.sendLocalizedMsg(player, "general.error.no-permission");
                        return true;
                    }

                    if (args.length != 2) {
                        chat.sendLocalizedMsg(player, "commands.erorr.invalid-arguments");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        chat.sendLocalizedMsg(player, "general.error.player-not-found");
                        return true;
                    }

                    scheduler.runAsync(new YggraTask() {
                        @Override
                        public void run() {
                            database.cooldowns().deleteByCriteria(QueryCondition.eq("playerId", target.getUniqueId()));
                            chat.sendLocalizedMsg(player, "commands.reset.reset-cooldowns-for", MapUtil.of("player", target.getName()));
                            chat.sendLocalizedMsg(target, "commands.reset.reseted-by-admin");
                        }
                    });
                    return true;
                }
            }

            // Invalid arguments
            chat.sendLocalizedMsg(player, "Commands.InvalidArguments");
            return true;
        }

        // Default to help command
        sendHelp(player, 1);
        return true;
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        switch (args.length){
            case 0:
            case 1: {
                return ListUtil.of("help", "reload", "npc", "remove-npcs", "give", "reset");
            }
            case 2: {
                String arg = args[0].toLowerCase();
                if (arg.equalsIgnoreCase("give") || arg.equalsIgnoreCase("reset"))
                    return null;
                return ListUtil.of();
            }
            case 3: {
                String arg = args[0].toLowerCase();
                if (arg.equalsIgnoreCase("give"))
                    return chestManager.getChests().values().stream().map(Chest::getKey).collect(Collectors.toList());
                return ListUtil.of();
            }
            default: {
                return ListUtil.of();
            }
        }
    }
}