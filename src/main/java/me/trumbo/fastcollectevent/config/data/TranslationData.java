package me.trumbo.fastcollectevent.config.data;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TranslationData {
    private Map<String, String> translations;

    public TranslationData() {
        translations = new HashMap<>();
    }

    public void load(FileConfiguration config) {
        translations.clear();
        ConfigurationSection translationSection = config.getConfigurationSection("item");
        if (translationSection != null) {
            for (String key : translationSection.getKeys(false)) {
                String value = translationSection.getString(key);
                if (value != null) {
                    translations.put(key.toUpperCase(), value);
                }
            }
        }
    }

    public String getTranslation(Material material) {
        String materialName = material.name();
        return translations.getOrDefault(materialName.toUpperCase(), materialName);
    }
}