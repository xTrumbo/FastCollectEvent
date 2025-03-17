package me.trumbo.fastcollectevent.managers;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ConfigManager {
    private final FastCollectEvent main;
    private File configFile;
    private File eventFile;
    private File itemTranslationsFile;
    private FileConfiguration config;
    private FileConfiguration event;
    private FileConfiguration itemTranslations;
    private MessageUtils.FormatType currentFormat;
    private Random random;

    public ConfigManager(FastCollectEvent main) {
        this.main = main;
        this.random = new Random();
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

    public <T> T getFromConfig(String configName, String sectionPath, String key, T defaultValue) {
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
                return defaultValue;
        }

        Object value;
        if (sectionPath != null && !sectionPath.isEmpty()) {
            ConfigurationSection section = targetConfig.getConfigurationSection(sectionPath);
            if (section == null) {
                return defaultValue;
            }
            value = section.get(key);
        } else {
            value = targetConfig.get(key);
        }

        if (value == null) {
            return defaultValue;
        }

        if (defaultValue instanceof List<?>) {
            return (T) value;
        }

        Class<?> targetType = defaultValue != null ? defaultValue.getClass() : Object.class;
        try {
            if (targetType == String.class) {
                return (T) value.toString();
            } else if (targetType == Integer.class) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                }
                return (T) Integer.valueOf(value.toString());
            } else if (targetType == Double.class) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                }
                return (T) Double.valueOf(value.toString());
            } else if (targetType == Boolean.class) {
                if (value instanceof Boolean) {
                    return (T) value;
                }
                return (T) Boolean.valueOf(value.toString());
            } else {
                return (T) targetType.cast(value);
            }
        } catch (NumberFormatException | ClassCastException e) {
            return defaultValue;
        }
    }

    public Object[] getRandomEventItem() {
        List<String> items = getFromConfig("event", "event", "items",
                new ArrayList<>(Arrays.asList("COBBLESTONE;100-500", "DIRT;100-500")));

        if (items.isEmpty() || random == null) {
            return new Object[]{Material.DIAMOND, 64};
        }

        String selectedItem = items.get(random.nextInt(items.size()));
        String[] parts = selectedItem.split(";");

        if (parts.length != 2) {
            return new Object[]{Material.DIAMOND, 64};
        }

        Material material = Material.matchMaterial(parts[0]);
        if (material == null) {
            material = Material.DIAMOND;
        }

        String[] range = parts[1].split("-");
        int amount = 64;
        if (range.length == 2) {
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                if (min <= max) {
                    amount = min + random.nextInt(max - min + 1);
                }
        }

        return new Object[]{material, amount};
    }

    public String getItemTranslation(Material material) {
        String materialName = material.name();
        String translation = getFromConfig("item_translations", "items", materialName, null);
        return translation;
    }

    private void loadFormat() {
        String formatString = config.getString("message-format", "HEX").toUpperCase();
        currentFormat = MessageUtils.FormatType.valueOf(formatString);
    }

    public MessageUtils.FormatType getCurrentFormat() {
        return currentFormat;
    }
}
