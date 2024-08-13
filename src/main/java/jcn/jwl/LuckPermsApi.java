package jcn.jwl;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class LuckPermsApi {
    private static LuckPerms api;
    private static JWhitelist plugin;
    private static boolean isInit;

    public static void init(LuckPerms api, JWhitelist plugin){
        LuckPermsApi.api = api;
        LuckPermsApi.plugin = plugin;
        isInit = true;
    }

    public static void removeGroupPermission(String playerName){
        if (!isInit) return;
        String group = plugin.getGroup();

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID playerUUID = player.getUniqueId();

        api.getUserManager().loadUser(playerUUID).thenAcceptAsync(user -> {
            if (user != null) {
                System.out.println(user.getUsername());
                InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().remove(node);
                api.getUserManager().saveUser(user);
            } else {
                plugin.getLogger().severe("Error: User not found.");
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error loading user: " + ex.getMessage());
            return null;
        });
    }

    public static void addGroupPermission(String playerName) {
        if (!isInit) return;
        String group = plugin.getGroup();

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID playerUUID = player.getUniqueId();

        api.getUserManager().loadUser(playerUUID).thenAcceptAsync(user -> {
            if (user != null) {
                System.out.println(user.getUsername());
                InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().add(node);
                api.getUserManager().saveUser(user);
            } else {
                plugin.getLogger().severe("Error: User not found.");
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error loading user: " + ex.getMessage());
            return null;
        });
    }
}
