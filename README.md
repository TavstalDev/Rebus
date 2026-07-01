# Rebus

![Release (latest by date)](https://img.shields.io/github/v/release/TavstalDev/Rebus?style=plastic-square)
![Workflow Status](https://img.shields.io/github/actions/workflow/status/TavstalDev/Rebus/ghrelease.yml?branch=stable&label=build&style=plastic-square)
![License](https://img.shields.io/github/license/TavstalDev/Rebus)
![Downloads](https://img.shields.io/github/downloads/TavstalDev/Rebus/total?style=plastic-square)
![Issues](https://img.shields.io/github/issues/TavstalDev/Rebus?style=plastic-square)

## Description
The remastered version of MesterMC's Rébusz plugin. 
Rebus is a lootbox plugin that allows players to open lootboxes and receive random rewards.

> **NOTE:** Currently, under mayor revisions.

## Dependencies

To ensure the plugin functions correctly, your server must have the following plugins installed:

- **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)**: This plugin is essential for handling custom network packets, which Rebus utilizes for its advanced features.
- **[Citizens](https://www.spigotmc.org/resources/citizens.13811/)**: This plugin allows for the creation and management of NPCs (Non-Player Characters) within the game, which Rebus uses to enhance player interaction.
- A plugin that integrates one of the following economy apis:
- - **[Vault](https://www.spigotmc.org/resources/vault.34315/)**: This plugin provides a unified API for various economy.
- - **[PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)**: This plugin allows for a point-based economy system, which Rebus can utilize for its lootbox transactions.

## Commands
| Command                             | Description                                          | Permission                       |
|-------------------------------------|------------------------------------------------------|----------------------------------|
| `/rebus help`                       | Displays help information about the plugin           | rebus.commands.rebus             |
| `/rebus version`                    | Displays the current version of the plugin           | rebus.commands.rebus             |
| `/rebus menu`                       | Opens the Rebus main menu GUI                        | rebus.commands.rebus.menu        |
| `/rebusadmin`                       | Main admin command.                                  | rebus.commands.rebusadmin        |
| `/rebusadmin help`                  | Displays help information about Rebus admin commands | rebus.commands.rebusadmin        |
| `/rebusadmin reload`                | Reloads the configuration files                      | rebus.commands.rebusadmin.reload | 
| `/rebusadmin version`               | Displays the current version of the plugin           | rebus.commands.rebusadmin.info   |
| `/rebusadmin npc`                   | Spawns the Rebus NPC                                 | rebus.commands.rebusadmin.npc    |
| `/rebusadmin give <player> <chest>` | Gives a specific chest to a player                   | rebus.commands.rebusadmin.give   |

`rebus.use` is the permission that allows players to interact with the npc and open the menu without having the `rebus.commands.rebus.menu` permission.

## Contributing

I welcome contributions! If you have ideas for features, bug fixes, or improvements, please consider contributing to the project.

1.  **Fork** the repository on GitHub.
2.  **Create a new branch** for your feature or bug fix (e.g., `feature/add-category` or `fix/gui-bug`).
3.  **Commit your changes** with clear, concise, and descriptive commit messages.
4.  **Push your branch** to your forked repository.
5.  **Open a Pull Request** to the `main` branch of this repository, describing your changes.

## License

This project is licensed under the **GNU General Public License v3.0**. You can find the full license text in the `LICENSE` file within this repository.

## Contact

For any questions, bug reports, or feature requests, please use the [GitHub issue tracker](https://github.com/TavstalDev/Rebus/issues).