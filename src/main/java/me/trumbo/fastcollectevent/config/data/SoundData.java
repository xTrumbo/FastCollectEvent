package me.trumbo.fastcollectevent.config.data;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SoundData {
    private final Map<String, SoundConfig> sounds = new HashMap<>();

    public void load(FileConfiguration config) {
        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                String name = soundsSection.getString(key + ".name");
                float volume = (float) soundsSection.getDouble(key + ".volume", 1.0);
                float pitch = (float) soundsSection.getDouble(key + ".pitch", 1.0);
                if (name != null) {
                    sounds.put(key, new SoundConfig(name, volume, pitch));
                }
            }
        }
    }

    public SoundConfig getSound(String key) {
        return sounds.get(key);
    }

    @Getter
    public static class SoundConfig {
        private final String soundName;
        private final float volume;
        private final float pitch;

        public SoundConfig(String soundName, float volume, float pitch) {
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}