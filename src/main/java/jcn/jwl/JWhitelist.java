package jcn.jwl;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class JWhitelist extends JavaPlugin {

    private JWhitelistDatabase databaseManager;
    private long checkInterval;
    private String kickMessage;
    private String group;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseManager = new JWhitelistDatabase(this);
        databaseManager.setupDatabase();

        loadConfigValues();

        getServer().getPluginManager().registerEvents(new JWhitelistListener(this), this);

        this.getCommand("jwhitelist").setExecutor(new JWhitelistCommand(this));

        startWhitelistCheckTask();

        if (getConfig().getBoolean("whitelist.luckperms_enable")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                LuckPerms api = provider.getProvider();
                LuckPermsApi.init(api, this);
                getLogger().info("LuckPerms successfully initialized.");
            } else {
                getLogger().warning("LuckPerms not found. Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    @Override
    public void onDisable() {
        databaseManager.closeDatabase();
    }

    public JWhitelistDatabase getDatabaseManager() {
        return databaseManager;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getGroup() {
        return group;
    }

    public void reloadConfiguration() {
        getServer().getScheduler().cancelTasks(this);
        loadConfigValues();
        startWhitelistCheckTask();
        databaseManager.closeDatabase();
        databaseManager.setupDatabase();
    }

    private void loadConfigValues() {
        checkInterval = parseDuration(getConfig().getString("whitelist.check_interval", "1h"));
        kickMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("whitelist.kick_message", "&cYou are not whitelisted on this server."));
        group = getConfig().getString("whitelist.group", "none");
    }

    private void startWhitelistCheckTask() {
        new WhitelistCheckTask().runTaskTimer(this, 20L, checkInterval / 50);
    }

    private class WhitelistCheckTask extends BukkitRunnable {
        @Override
        public void run() {
            databaseManager.cleanupWhitelist();
        }
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
}