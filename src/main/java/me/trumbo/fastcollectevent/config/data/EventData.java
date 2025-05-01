package me.trumbo.fastcollectevent.config.data;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class EventData {
    private int startDelay;
    private int duration;
    private boolean fireworkOnWinner;
    private List<String> startMessage = new ArrayList<>();
    private List<String> eventEnd = new ArrayList<>();
    private List<String> timeEnd = new ArrayList<>();
    private List<String> eventEndNoWinner = new ArrayList<>();
    private List<String> halfReached = new ArrayList<>();
    private List<String> items = new ArrayList<>();
    private Map<Integer, List<String>> topRewards = new HashMap<>();
    private SoundData soundData;

    public void load(FileConfiguration config) {
        ConfigurationSection eventSection = config.getConfigurationSection("event");
        if (eventSection != null) {
            startDelay = eventSection.getInt("start-delay", 0);
            duration = eventSection.getInt("duration", 0);
            fireworkOnWinner = eventSection.getBoolean("firework-on-winner", false);
            startMessage = eventSection.getStringList("start-message");
            eventEnd = eventSection.getStringList("event-end");
            timeEnd = eventSection.getStringList("time-end");
            eventEndNoWinner = eventSection.getStringList("event-end-no-winner");
            halfReached = eventSection.getStringList("half-reached");
            items = eventSection.getStringList("items");
        }

        ConfigurationSection topRewardsSection = config.getConfigurationSection("top-rewards");
        topRewards = new HashMap<>();
        if (topRewardsSection != null) {
            for (String place : topRewardsSection.getKeys(false)) {
                    int position = Integer.parseInt(place);
                    List<String> rewards = topRewardsSection.getStringList(place);
                    topRewards.put(position, rewards);
            }
        }
    }
}