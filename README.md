# Rebus

![Release (latest by date)](https://img.shields.io/github/v/release/TavstalDev/Rebus?style=plastic-square)
![Workflow Status](https://img.shields.io/github/actions/workflow/status/TavstalDev/Rebus/ghrelease.yml?branch=stable&label=build&style=plastic-square)
![License](https://img.shields.io/github/license/TavstalDev/Rebus)
![Downloads](https://img.shields.io/github/downloads/TavstalDev/Rebus/total?style=plastic-square)
![Issues](https://img.shields.io/github/issues/TavstalDev/Rebus?style=plastic-square)

## Description
The remastered version of MesterMC's Rebus plugin.

## Dependencies

To ensure the plugin functions correctly, your server must have the following plugins installed:

- **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)**: This plugin is essential for handling custom network packets, which Rebus utilizes for its advanced features.
- **[Citizens](https://www.spigotmc.org/resources/citizens.13811/)**: This plugin allows for the creation and management of NPCs (Non-Player Characters) within the game, which Rebus uses to enhance player interaction.
- **[Vault](https://www.spigotmc.org/resources/vault.34315/)**: This plugin provides a unified API for various economy.

## Commands
| Command                             | Description                                          | Permission   |
|-------------------------------------|------------------------------------------------------|--------------|
| `/rebus help`                       | Displays help information about Rebus                | rebus.use    |
| `/rebus version`                    | Displays the current version of Rebus                | rebus.use    |
| `/rebus menu`                       | Opens the Rebus main menu GUI                        | rebus.use    |
| `/rebusadmin`                       | Main admin command.                                  | rebus.admin  |
| `/rebusadmin help`                  | Displays help information about Rebus admin commands | rebus.help   |
| `/rebusadmin reload`                | Reloads the Rebus configuration files                | rebus.reload | 
| `/rebusadmin version`               | Displays the current version of Rebus                | rebus.info   |
| `/rebusadmin npc`                   | Spawns the Rebus NPC                                 | rebus.npc    |
| `/rebusadmin give <player> <chest>` | Gives a specific puzzle to a player                  | rebus.give   |

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