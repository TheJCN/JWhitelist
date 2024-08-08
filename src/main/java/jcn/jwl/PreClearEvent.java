package jcn.jwl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PreClearEvent implements Listener {

    private final JWhitelist plugin;
    private final List<String> playersToRemove;

    public PreClearEvent(JWhitelist plugin, List<String> playersToRemove) {
        this.plugin = plugin;
        this.playersToRemove = playersToRemove;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();

        if (command.equalsIgnoreCase("/jwhitelist confirm") || command.equalsIgnoreCase("/jwl confirm")) {
            event.setCancelled(true);
            executeClear();
            player.sendMessage("Whitelist cleared successfully.");
        } else if (command.equalsIgnoreCase("/jwhitelist cancel") || command.equalsIgnoreCase("/jwl cancel")) {
            event.setCancelled(true);
            player.sendMessage("Whitelist clearing canceled.");
        }
    }

    private void executeClear() {
        JWhitelistDatabase databaseManager = plugin.getDatabaseManager();
        for (String playerName : playersToRemove) {
            if (Bukkit.getPlayer(playerName) == null) databaseManager.removePlayerFromWhitelist(playerName);
        }
        saveRemovedPlayersToFile();
    }

    private void saveRemovedPlayersToFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fileName = "removed_players_" + sdf.format(new Date()) + ".txt";
        try (FileWriter writer = new FileWriter(plugin.getDataFolder() + "/" + fileName)) {
            for (String playerName : playersToRemove) {
                writer.write(playerName + "\n");
            }
            System.out.println("Remove player successful saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
