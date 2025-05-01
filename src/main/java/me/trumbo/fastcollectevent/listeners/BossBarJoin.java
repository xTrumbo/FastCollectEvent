package me.trumbo.fastcollectevent.listeners;

import me.trumbo.fastcollectevent.FastCollectEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BossBarJoin implements Listener {

    private final FastCollectEvent main;

    public BossBarJoin(FastCollectEvent main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (main.getEventManager().isEventActive() && main.getBossBarManager().isEnabled()) {
            main.getBossBarManager().addPlayer(event.getPlayer());
        }
    }
}
