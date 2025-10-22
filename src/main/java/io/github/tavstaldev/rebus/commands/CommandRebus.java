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

public class CommandRebus implements CommandExecutor {
    private final PluginLogger _logger = Rebus.logger().withModule(CommandRebus.class);

    private final List<SubCommandData> _subCommands = new ArrayList<>() {
        {
            // HELP
            add(new SubCommandData("help", "rebus.admin", Map.of(
                    "syntax", "",
                    "description", "Commands.Help.Desc"
            )));
            // VERSION
            add(new SubCommandData("version", "rebus.info", Map.of(
                    "syntax", "",
                    "description", "Commands.Version.Desc"
            )));
            // MENU
            add(new SubCommandData("menu", "rebus.use", Map.of(
                    "syntax", "",
                    "description", "Commands.Menu.Desc"
            )));
        }
    };

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (sender instanceof ConsoleCommandSender) {
            _logger.info(ChatUtils.translateColors("Commands.ConsoleCaller", true).toString());
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("rebus.use")) {
            Rebus.Instance.sendLocalizedMsg(player, "General.NoPermission");
            return true;
        }


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

            Rebus.Instance.sendLocalizedMsg(player, "Commands.InvalidArguments");
            return true;
        }

        help(player, 1);
        return true;
    }

    private void help(Player player, int page) {
        int maxPage = 1 + (_subCommands.size() / 15);

        if (page > maxPage)
            page = maxPage;
        if (page < 1)
            page = 1;
        int finalPage = page;

        Rebus.Instance.sendLocalizedMsg(player, "Commands.Help.Title", Map.of(
                        "current_page", finalPage,
                        "max_page", maxPage
                )
        );
        Rebus.Instance.sendLocalizedMsg(player, "Commands.Help.Info");

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

        // Bottom message
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
