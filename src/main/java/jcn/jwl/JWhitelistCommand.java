package jcn.jwl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.util.Arrays;
import java.util.List;

public class JWhitelistCommand implements CommandExecutor, TabCompleter {
    private final JWhitelistDatabase databaseManager;
    private final JWhitelist plugin;

    public JWhitelistCommand(JWhitelist plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (sender.hasPermission("jwhitelist.reload") || sender instanceof ConsoleCommandSender) {
                    plugin.reloadConfig();
                    plugin.reloadConfiguration();
                    sender.sendMessage(plugin.getMessage("reload_success"));
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            case "clear":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /jwhitelist clear <time>");
                    return true;
                }
                if (sender.hasPermission("jwhitelist.clear") || sender instanceof ConsoleCommandSender) {
                    long clearDuration = parseDuration(args[1]);
                    if (clearDuration == -1) {
                        sender.sendMessage(plugin.getMessage("invalid_time_format"));
                        return true;
                    }
                    clearWhitelist(sender, clearDuration);
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /jwhitelist add <player> [time]");
                    return true;
                }
                String playerName = args[1];
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (sender.hasPermission("jwhitelist.add") || sender instanceof ConsoleCommandSender) {
                    long expiry = (args.length == 3) ? parseDuration(args[2]) : -1;
                    if (expiry == -1 && args.length == 3) {
                        sender.sendMessage(plugin.getMessage("invalid_time_format"));
                        return true;
                    }
                    databaseManager.addPlayerToWhitelist(player, expiry);
                    if (expiry == -1) {
                        sender.sendMessage(plugin.getMessage("player_added").replace("%player%", playerName));
                    } else {
                        sender.sendMessage(plugin.getMessage("player_added_temp").replace("%player%", playerName).replace("%time%", args[2]));
                    }
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /jwhitelist remove <player>");
                    return true;
                }
                playerName = args[1];
                player = Bukkit.getOfflinePlayer(playerName);
                if (sender.hasPermission("jwhitelist.remove") || sender instanceof ConsoleCommandSender) {
                    databaseManager.removePlayerFromWhitelist(playerName);
                    sender.sendMessage(plugin.getMessage("player_removed").replace("%player%", playerName));
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            default:
                sender.sendMessage("Unknown subcommand: " + subCommand);
                return false;
        }
    }

    private long parseDuration(String duration) {
        try {
            char unit;
            long time;
            if (duration.endsWith("min")) {
                unit = duration.charAt(duration.length() - 3);
                time = Long.parseLong(duration.substring(0, duration.length() - 3));
            } else {
                unit = duration.charAt(duration.length() - 1);
                time = Long.parseLong(duration.substring(0, duration.length() - 1));
            }
            return switch (unit) {
                case 's' -> time * 1000;
                case 'm' -> duration.endsWith("min") ? time * 60 * 1000 : time * 30 * 24 * 60 * 60 * 1000;
                case 'h' -> time * 60 * 60 * 1000;
                case 'd' -> time * 24 * 60 * 60 * 1000;
                default -> -1;
            };
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void clearWhitelist(CommandSender sender, long clearDuration) {
        List<String> playersToRemove = databaseManager.getPlayersToRemove(clearDuration);
        if (playersToRemove.isEmpty()) {
            sender.sendMessage(plugin.getMessage("no_players_to_remove"));
            return;
        }

        sender.sendMessage(plugin.getMessage("players_to_remove_found").replace("%count%", String.valueOf(playersToRemove.size())));
        sender.sendMessage(plugin.getMessage("confirm_clear"));
        Bukkit.getServer().getPluginManager().registerEvents(new PreClearEvent(plugin, playersToRemove), plugin);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "clear", "add", "remove");
        }
        return null;
    }
}
