package me.trumbo.fastcollectevent;

import me.trumbo.fastcollectevent.commands.MainCommand;
import me.trumbo.fastcollectevent.commands.MainCompleter;
import me.trumbo.fastcollectevent.managers.ConfigManager;
import me.trumbo.fastcollectevent.managers.EventManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastCollectEvent extends JavaPlugin {

    private ConfigManager configManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {

        configManager = new ConfigManager(this);
        eventManager = new EventManager(this);

        getCommand("fce").setExecutor(new MainCommand(this));
        getCommand("fce").setTabCompleter(new MainCompleter(this));

        new Metrics(this, 25139);

    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

}

