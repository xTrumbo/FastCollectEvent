package me.trumbo.fastcollectevent.utils;

import me.trumbo.fastcollectevent.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtils {

    private static class SoundConfig {
        final String soundName;
        final float volume;
        final float pitch;

        SoundConfig(String soundName, float volume, float pitch) {
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
        }

        static SoundConfig loadFromConfig(String soundKey, ConfigManager configManager) {

            if (soundKey == null || configManager == null) {
                return null;
            }

            String soundName = configManager.getFromConfig("event", "sounds", soundKey + ".name");
            float volume = configManager.getFromConfig("event", "sounds", soundKey + ".volume");
            float pitch = configManager.getFromConfig("event", "sounds", soundKey + ".pitch");

            return new SoundConfig(soundName, volume, pitch);

        }
    }

    public static void playSound(Player player, String soundKey, ConfigManager configManager) {

        if (player == null) return;

        SoundConfig config = SoundConfig.loadFromConfig(soundKey, configManager);

        Sound sound = Sound.valueOf(config.soundName.toUpperCase());
        player.playSound(player.getLocation(), sound, config.volume, config.pitch);

    }


    public static void playSoundToAll(String soundKey, ConfigManager configManager) {

        SoundConfig config = SoundConfig.loadFromConfig(soundKey, configManager);

            Sound sound = Sound.valueOf(config.soundName.toUpperCase());

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, config.volume, config.pitch);
            }

    }
}

