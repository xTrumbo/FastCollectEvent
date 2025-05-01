package me.trumbo.fastcollectevent.config;

import lombok.Getter;
import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.config.data.ConfigData;
import me.trumbo.fastcollectevent.config.data.EventData;
import me.trumbo.fastcollectevent.config.data.SoundData;
import me.trumbo.fastcollectevent.config.data.TranslationData;
import me.trumbo.fastcollectevent.utils.RandomUtils;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
    public class Config {

        private FastCollectEvent main;
        private File configFile;
        private File eventFile;
        private File itemTranslationsFile;
        private FileConfiguration config;
        private FileConfiguration event;
        private FileConfiguration itemTranslations;

        private ConfigData configData;
        private EventData eventData;
        private SoundData soundData;
        private TranslationData translationData;

        public Config(FastCollectEvent main) {
            this.main = main;
            this.configData = new ConfigData();
            this.eventData = new EventData();
            this.soundData = new SoundData();
            this.translationData = new TranslationData();

            createFiles();
        }

        public void createFiles() {
            if (!main.getDataFolder().exists()) {
                main.getDataFolder().mkdir();
            }

            configFile = new File(main.getDataFolder(), "config.yml");
            eventFile = new File(main.getDataFolder(), "event.yml");
            itemTranslationsFile = new File(main.getDataFolder(), "item_translations.yml");

            if (!configFile.exists()) {
                main.saveResource("config.yml", false);
            }
            if (!eventFile.exists()) {
                main.saveResource("event.yml", false);
            }
            if (!itemTranslationsFile.exists()) {
                main.saveResource("item_translations.yml", false);
            }

            config = YamlConfiguration.loadConfiguration(configFile);
            event = YamlConfiguration.loadConfiguration(eventFile);
            itemTranslations = YamlConfiguration.loadConfiguration(itemTranslationsFile);

            configData.load(config);
            eventData.load(event);
            soundData.load(event);
            translationData.load(itemTranslations);


        }

    public void addEventItem(Material itemType, String range, String translation) {
        try {
            List<String> items = event.getStringList("event.items");
            String itemEntry = itemType.name() + ";" + range;
            if (!items.contains(itemEntry)) {
                items.add(itemEntry);
                event.set("event.items", items);
                event.save(eventFile);
                event.load(eventFile);
            }

            itemTranslations.set("item." + itemType.name(), translation);
            itemTranslations.save(itemTranslationsFile);
            itemTranslations.load(itemTranslationsFile);

            eventData.load(event);
            translationData.load(itemTranslations);
        } catch (IOException | InvalidConfigurationException e) {
            main.getLogger().severe(e.getMessage());
        }
    }

    public Object[] getRandomEventItem() {
        List<String> items = eventData.getItems();
        if (items == null || items.isEmpty()) {
            return new Object[]{Material.DIAMOND, 64};
        }
        String randomItem = RandomUtils.getRandomElement(items);
        return RandomUtils.getRandomEventItem(randomItem);
    }

    public List<Material> getEventItems() {
        List<Material> materials = new ArrayList<>();
        for (String item : eventData.getItems()) {
            String[] parts = item.split(";");
            if (parts.length == 2) {
                Material material = Material.matchMaterial(parts[0]);
                if (material != null) {
                    materials.add(material);
                }
            }
        }
        return materials;
    }

    public Map<Integer, List<String>> getTopRewards() {
        return eventData.getTopRewards();
    }

}
