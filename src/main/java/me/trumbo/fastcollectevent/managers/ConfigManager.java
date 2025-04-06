package me.trumbo.fastcollectevent.managers;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import me.trumbo.fastcollectevent.utils.RandomUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {
    private final FastCollectEvent main;
    private File configFile;
    private File eventFile;
    private File itemTranslationsFile;
    private FileConfiguration config;
    private FileConfiguration event;
    private FileConfiguration itemTranslations;

    public ConfigManager(FastCollectEvent main) {
        this.main = main;
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

        loadFormat();
    }

    public <T> T getFromConfig(String configName, String sectionPath, String key) {
        FileConfiguration targetConfig;
        switch (configName.toLowerCase()) {
            case "config":
                targetConfig = config;
                break;
            case "event":
                targetConfig = event;
                break;
            case "item_translations":
                targetConfig = itemTranslations;
                break;
            default:
                return null;
        }

        Object value;
        if (sectionPath != null && !sectionPath.isEmpty()) {
            ConfigurationSection section = targetConfig.getConfigurationSection(sectionPath);
            if (section == null) {
                return null;
            }
            value = section.get(key);
        } else {
            value = targetConfig.get(key);
        }

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            Number number = (Number) value;
            if (number instanceof Integer) {
                return (T) number;
            } else if (number instanceof Double || number instanceof Float) {
                return (T) Float.valueOf(number.floatValue());
            } else {
                return (T) number;
            }
        } else if (value instanceof String) {
            return (T) value;
        } else if (value instanceof Boolean) {
            return (T) value;
        } else {
            return (T) value;
        }
    }

    public void addEventItem(Material material, String range, String translation) {

        List<String> items = getFromConfig("event", "event", "items");
        if (items == null) {
            items = new ArrayList<>();
        }

        String itemEntry = material.name() + ";" + range;
        if (!items.contains(itemEntry)) {
            items.add(itemEntry);
            event.set("event.items", items);
        }

        itemTranslations.set("items." + material.name(), translation);

        try {
            event.save(eventFile);
            itemTranslations.save(itemTranslationsFile);
        } catch (IOException e) {
            main.getLogger().severe(e.getMessage());
        }
    }

    public Object[] getRandomEventItem() {
        List<String> items = getFromConfig("event", "event", "items");
        return RandomUtils.getRandomEventItem(items);
    }

    public List<Map.Entry<Integer, List<String>>> getTopRewards() {
        List<Map.Entry<Integer, List<String>>> rewards = new ArrayList<>();
        ConfigurationSection section = event.getConfigurationSection("top-rewards");

        for (String key : section.getKeys(false)) {
            int position = Integer.parseInt(key);
            Object rewardObj = getFromConfig("event", "top-rewards", key);
            List<String> commands = rewardObj instanceof List ? (List<String>) rewardObj : Arrays.asList((String) rewardObj);
            rewards.add(new AbstractMap.SimpleEntry<>(position, commands));
        }

        return rewards;
    }

    public List<Material> getEventItems() {
        List<String> items = getFromConfig("event", "event", "items");

        List<Material> materials = new ArrayList<>();
        for (String item : items) {
            String[] parts = item.split(";");
            if (parts.length > 0) {
                Material material = Material.matchMaterial(parts[0]);
                if (material != null && material.isItem()) {
                    materials.add(material);
                }
            }
        }
        return materials;
    }

    public String getItemTranslation(Material material) {
        String materialName = material.name();
        return getFromConfig("item_translations", "items", materialName);
    }

    private void loadFormat() {
        String formatString = config.getString("message-format");
        if (formatString == null) {
            MessageUtils.setFormat(MessageUtils.FormatType.HEX);
            return;
        }
        try {
            MessageUtils.setFormat(MessageUtils.FormatType.valueOf(formatString.toUpperCase()));
        } catch (IllegalArgumentException e) {
            MessageUtils.setFormat(MessageUtils.FormatType.HEX);
        }
    }
}
