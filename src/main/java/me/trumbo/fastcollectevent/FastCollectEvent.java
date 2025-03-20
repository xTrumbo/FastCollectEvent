package me.trumbo.fastcollectevent;

import me.trumbo.fastcollectevent.commands.MainCommand;
import me.trumbo.fastcollectevent.commands.MainCompleter;
import me.trumbo.fastcollectevent.managers.ConfigManager;
import me.trumbo.fastcollectevent.managers.DatabaseManager;
import me.trumbo.fastcollectevent.managers.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastCollectEvent extends JavaPlugin {

    private ConfigManager configManager;
    private EventManager eventManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {

        configManager = new ConfigManager(this);
        eventManager = new EventManager(this);
        databaseManager = new DatabaseManager(this);

        getCommand("fce").setExecutor(new MainCommand(this));
        getCommand("fce").setTabCompleter(new MainCompleter(this));

        new Metrics(this, 25152);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
        }
    }

    @Override
    public void onDisable() {
        databaseManager.close();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

}

