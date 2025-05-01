package me.trumbo.fastcollectevent;

import me.trumbo.fastcollectevent.commands.MainCommand;
import me.trumbo.fastcollectevent.commands.MainCompleter;
import me.trumbo.fastcollectevent.config.Config;
import me.trumbo.fastcollectevent.listeners.BossBarJoin;
import me.trumbo.fastcollectevent.managers.BossBarManager;
import me.trumbo.fastcollectevent.managers.DatabaseManager;
import me.trumbo.fastcollectevent.managers.EventManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastCollectEvent extends JavaPlugin {

    private Config config;
    private EventManager eventManager;
    private DatabaseManager databaseManager;
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {

        config = new Config(this);
        eventManager = new EventManager(this);
        databaseManager = new DatabaseManager(this);
        bossBarManager = new BossBarManager(this);

        getCommand("fce").setExecutor(new MainCommand(this));
        getCommand("fce").setTabCompleter(new MainCompleter(this));

        new Metrics(this, 25152);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
        }

        getServer().getPluginManager().registerEvents(new BossBarJoin(this), this);
    }

    @Override
    public void onDisable() {
        databaseManager.close();
    }

    public Config getPluginConfig() {
        return config;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

}

