package jcn.jwl;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                    plugin.reloadConfiguration();
                    sender.sendMessage(plugin.getMessage("reload_success"));
                }
                else {
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
                }
                else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /jwhitelist add <player> [time]");
                    return true;
                }
                if (sender.hasPermission("jwhitelist.add") || sender instanceof ConsoleCommandSender) {
                    String playerName = args[1];
                    long expiry = (args.length == 3) ? parseDuration(args[2]) : -1;
                    if (expiry == -1 && args.length == 3) {
                        sender.sendMessage(plugin.getMessage("invalid_time_format"));
                        return true;
                    }
                    databaseManager.addPlayerToWhitelist(playerName, expiry);
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
                if (sender.hasPermission("jwhitelist.remove") || sender instanceof ConsoleCommandSender) {
                    String playerName = args[1];
                    databaseManager.removePlayerFromWhitelist(playerName);
                    sender.sendMessage(plugin.getMessage("player_removed").replace("%player%", playerName));
                    Player playerOnline = Bukkit.getPlayer(playerName);
                    if (playerOnline != null && playerOnline.isOnline()) {
                        playerOnline.kickPlayer(plugin.getMessage("player_kicked"));
                    }
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            case "list":
                if (sender.hasPermission("jwhitelist.list") || sender instanceof ConsoleCommandSender) {
                    Map<String, Long> whitelistedPlayers = databaseManager.getAllWhitelistedPlayers();
                    if (whitelistedPlayers.isEmpty()) {
                        sender.sendMessage(plugin.getMessage("whitelist_is_empty"));
                    } else {
                        sender.sendMessage(plugin.getMessage("whitelisted_players"));
                        for (Map.Entry<String, Long> entry : whitelistedPlayers.entrySet()) {
                            String name = entry.getKey();
                            long remain = entry.getValue();
                            String timeString;
                            if (remain < -1) {
                                timeString = plugin.getMessage("expired");
                            } else if (remain == -1) {
                                timeString = plugin.getMessage("expires_never");
                            } else {
                                timeString = plugin.getMessage("expires_in").replace("%time%", formatDuration(remain));
                            }
                            sender.sendMessage("- " + name + " (" + timeString + ")");
                        }
                    }
                } else {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                }
                return true;

            default:
                sender.sendMessage(plugin.getMessage("unknown_subcommand").replace("%subcommand%", subCommand));
                return false;
        }
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis == -1L) {
            return "Never expires";
        } else if (durationMillis <= 0) {
            return "Expired";
        }

        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;

        if (months > 0) {
            return months + "m";
        } else if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "min";
        } else {
            return seconds + "s";
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
            return Arrays.asList("add", "remove", "reload", "clear", "list");
        } else if (args.length == 2){
            if (args[0].equalsIgnoreCase("add")) return Arrays.asList(sender.getName());
            else if (args[0].equalsIgnoreCase("remove")) return databaseManager.getAllPlayerNameInWhitelist();
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }
}
