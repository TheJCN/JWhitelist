package jcn.jwl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JWhitelistListener implements Listener {

    private final JWhitelist plugin;
    private final JWhitelistDatabase databaseManager;

    public JWhitelistListener(JWhitelist plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getName());
        if (databaseManager.isPlayerWhitelisted(player)) {
            databaseManager.updateLastLogin(player);
        } else {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, plugin.getKickMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            LuckPermsApi.addPermission(player.getName());
        }
    }
}
