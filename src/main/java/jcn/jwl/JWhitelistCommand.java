package jcn.jwl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
                    plugin.reloadConfig();
                    plugin.reloadConfiguration();
                    sender.sendMessage("Whitelist configuration reloaded successfully.");
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
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
                        sender.sendMessage("Invalid time format. Use format like 5s, 5min, 5h, 5d, 5m");
                        return true;
                    }
                    clearWhitelist(sender, clearDuration);
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
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
                    long expiry = (args.length == 3) ? parseDuration(args[2]) : 0;
                    if (expiry > 0 || args.length == 2) {
                        databaseManager.addPlayerToWhitelist(player, (expiry > 0) ? System.currentTimeMillis() + expiry : 0);
                        sender.sendMessage("Player " + playerName + (expiry > 0 ? " added to temporary whitelist for " + args[2] + "." : " added to whitelist."));
                    } else {
                        sender.sendMessage("Invalid time format. Use format like 5s, 5min, 5h, 5d, 5m");
                    }
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
                }
                return true;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /jwhitelist remove <player>");
                    return true;
                }
                String removePlayerName = args[1];
                if (sender.hasPermission("jwhitelist.remove") || sender instanceof ConsoleCommandSender) {
                    databaseManager.removePlayerFromWhitelist(removePlayerName);
                    sender.sendMessage("Player " + removePlayerName + " removed from whitelist.");
                    Player players_remove = Bukkit.getPlayer(removePlayerName);
                    if (players_remove != null && players_remove.isOnline()) players_remove.kickPlayer("Whitelist remove");
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
                }
                return true;

            case "list":
                if (sender.hasPermission("jwhitelist.list") || sender instanceof ConsoleCommandSender) {
                    Map<String, Long> whitelistedPlayers = databaseManager.getAllWhitelistedPlayers();
                    if (whitelistedPlayers.isEmpty()) {
                        sender.sendMessage("The whitelist is empty.");
                    } else {
                        sender.sendMessage("Whitelisted players:");
                        for (Map.Entry<String, Long> entry : whitelistedPlayers.entrySet()) {
                            String name = entry.getKey();
                            long remain = entry.getValue();
                            String timeString;
                            if (remain < -1) {
                                timeString = "expired";
                            } else if (remain == -1) {
                                timeString = "expires never";
                            } else {
                                timeString = "expires in " + formatDuration(remain);
                            }
                            sender.sendMessage("- " + name + " (" + timeString + ")");
                        }

                    }
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
                }
                return true;

            default:
                return false;
        }
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
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


    private void clearWhitelist(CommandSender sender, long clearDuration) {
        List<String> playersToRemove = databaseManager.getPlayersToRemove(clearDuration);
        if (playersToRemove.isEmpty()) {
            sender.sendMessage("No players need to be removed.");
            return;
        }

        sender.sendMessage("Found " + playersToRemove.size() + " players to remove.");
        sender.sendMessage("Type /jwhitelist confirm to confirm or /jwhitelist cancel to cancel.");

        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().registerEvents(new PreClearEvent(plugin, playersToRemove), plugin));
    }

    private long parseDuration(String duration) {
        try {
            char unit;
            long time;
            if (duration.endsWith("min")) {
                unit = duration.charAt(duration.length() - 3);
                time = Long.parseLong(duration.substring(0, duration.length() - 3));
            }
            else {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("add", "remove", "reload", "clear", "list"));
        } else if (args.length == 2){
            if (args[0].equalsIgnoreCase("add")) return new ArrayList<>(List.of(sender.getName()));
            else if (args[0].equalsIgnoreCase("remove")) return databaseManager.getAllPlayerNameInWhitelist();
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }
}
