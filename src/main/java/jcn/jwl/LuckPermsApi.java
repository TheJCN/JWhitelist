package jcn.jwl;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

public class LuckPermsApi {
    private static LuckPerms api;
    private static JWhitelist plugin;
    private static boolean isInit;

    public static void init(LuckPerms api, JWhitelist plugin){
        LuckPermsApi.api = api;
        LuckPermsApi.plugin = plugin;
        isInit = true;
    }

    public static void removePermission(String playerName){
        if (!isInit) return;
        String group = plugin.getGroup();

        User user = api.getUserManager().getUser(playerName);
        if (user != null) {
            InheritanceNode node = InheritanceNode.builder(group).build();
            user.data().remove(node);
            api.getUserManager().saveUser(user);
        }
        else {
            plugin.getLogger().severe("LuckyPerms couldn't find the player. Player groups were not modified.");
        }
    }

    public static void addPermission(String playerName) {
        if (!isInit) return;
        String group = plugin.getGroup();

        User user = api.getUserManager().getUser(playerName);
        if (user != null) {
            InheritanceNode node = InheritanceNode.builder(group).build();
            user.data().add(node);
            api.getUserManager().saveUser(user);
        }
        else {
            plugin.getLogger().severe("LuckyPerms couldn't find the player. Player groups were not modified.");
        }
    }
}
