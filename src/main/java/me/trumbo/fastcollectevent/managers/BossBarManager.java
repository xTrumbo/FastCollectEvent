package me.trumbo.fastcollectevent.managers;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossBarManager {

    private final FastCollectEvent main;
    private BossBar bossBar;
    private boolean enabled;

    public BossBarManager(FastCollectEvent main) {
        this.main = main;

        this.enabled = main.getConfigManager().getFromConfig("config", "bossbar", "enabled");

    }

    public void createBossBar(String title, BarColor color, BarStyle style) {
        if (bossBar != null) {
            removeBossBar();
        }
        bossBar = main.getServer().createBossBar(title, color, style);
    }

    public void updateProgress(double progress) {
        if (bossBar != null) {
            bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
        }
    }

    public void showToAllPlayers() {
        if (bossBar != null) {
            for (Player player : main.getServer().getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        }
    }

    public void removeFromAllPlayers() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void removeBossBar() {
        if (bossBar != null) {
            removeFromAllPlayers();
            bossBar = null;
        }
    }

    public String getTitle(String itemTranslation) {
        String title = ((String) main.getConfigManager().getFromConfig("config", "bossbar", "title"))
                .replace("%item%", itemTranslation);
        Object formattedTitle = MessageUtils.format(title);
        if (formattedTitle instanceof String) {
            return (String) formattedTitle;
        }
        return title;
    }
    

    public BarColor getColor() {
        return BarColor.valueOf((String) main.getConfigManager().getFromConfig("config", "bossbar", "color"));
    }

    public BarStyle getStyle() {
        return BarStyle.valueOf((String) main.getConfigManager().getFromConfig("config", "bossbar", "style"));
    }

    public boolean isEnabled() {
        return enabled;
    }
}