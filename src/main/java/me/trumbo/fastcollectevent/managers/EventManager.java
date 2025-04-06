package me.trumbo.fastcollectevent.managers;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import me.trumbo.fastcollectevent.utils.RandomUtils;
import me.trumbo.fastcollectevent.utils.SoundUtils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class EventManager {

    private FastCollectEvent main;
    private BukkitTask delayTimer;
    private BukkitTask eventTimer;
    private boolean isEventActive;

    private HashMap<UUID, Integer> playerProgress;

    private Material targetItem;
    private int targetAmount;

    private long delayStartTime;
    private long eventStartTime;
    private long delayDuration;
    private long eventDuration;

    private long customEventDuration;

    private String lastWinner;

    public EventManager(FastCollectEvent main) {
        this.isEventActive = false;
        this.main = main;
        this.playerProgress = new HashMap<>();

        this.lastWinner = null;

        this.customEventDuration = -1;

        startDelayTimer();
    }

    private void startDelayTimer() {
        Object[] itemData = main.getConfigManager().getRandomEventItem();
        this.targetItem = (Material) itemData[0];
        this.targetAmount = (int) itemData[1];

        int delaySeconds = main.getConfigManager().getFromConfig("event", "event", "start-delay");
        delayDuration = delaySeconds * 20L;

        delayTimer = main.getServer().getScheduler().runTaskLater(main, () -> {
            startEventTimer();
        }, delayDuration);
        delayStartTime = main.getServer().getCurrentTick();
    }

    public void startEventTimer() {
        ConfigManager configManager = main.getConfigManager();
        int eventDurationSeconds = customEventDuration >= 0
                ? (int) (customEventDuration / 20)
                : configManager.getFromConfig("event", "event", "duration");

        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

        eventDuration = eventDurationSeconds * 20L;
        isEventActive = true;

        SoundUtils.playSoundToAll("event-start", main.getConfigManager());

        List<String> startMessages = configManager.getFromConfig("event", "event", "start-message");
        for (String message : startMessages) {
            String formattedMessage = message.replace("%item%", itemTranslation)
                    .replace("%int%", String.valueOf(targetAmount));
            MessageUtils.sendMessageToAll(formattedMessage);
        }

        eventTimer = main.getServer().getScheduler().runTaskLater(main, () -> {
            endEvent(null);
        }, eventDuration);
        eventStartTime = main.getServer().getCurrentTick();

        customEventDuration = -1;
    }

    public void endEvent(Player winner) {
        isEventActive = false;
        ConfigManager configManager = main.getConfigManager();
        String itemTranslation = configManager.getItemTranslation(targetItem);

        String winnerName = null;
        int winnerProgress = 0;
        Player winnerPlayer = null;

        if (winner != null) {
            winnerName = winner.getName();
            lastWinner = winnerName;
            winnerPlayer = winner;
            winnerProgress = playerProgress.get(winner.getUniqueId());
        }

        else if (!playerProgress.isEmpty()) {
            UUID winnerUUID = null;
            int maxProgress = -1;
            for (Map.Entry<UUID, Integer> entry : playerProgress.entrySet()) {
                if (entry.getValue() > maxProgress) {
                    maxProgress = entry.getValue();
                    winnerUUID = entry.getKey();
                }
            }
            winnerProgress = maxProgress;
            winnerName = main.getServer().getOfflinePlayer(winnerUUID).getName();
            if (winnerName == null) winnerName = "Неизвестный игрок";
            winnerPlayer = main.getServer().getPlayer(winnerUUID);
        }

        if (winnerName != null) {
            SoundUtils.playSoundToAll("end-yes-winner", configManager);

            List<String> endMessages;
            if (winner != null) {
                endMessages = configManager.getFromConfig("event", "event", "event-end");
            } else {
                endMessages = configManager.getFromConfig("event", "event", "time-end");
            }

            for (String message : endMessages) {
                String formattedMessage = message.replace("%item%", itemTranslation)
                        .replace("%int%", String.valueOf(targetAmount))
                        .replace("%winner%", winnerName)
                        .replace("%amount%", String.valueOf(winnerProgress));
                MessageUtils.sendMessageToAll(formattedMessage);
            }

            boolean fireworkEnabled = configManager.getFromConfig("event", "event", "firework-on-winner");
            if (fireworkEnabled && winnerPlayer != null && winnerPlayer.isOnline()) {
                spawnRandomFirework(winnerPlayer.getLocation());
            }

            int topLines = configManager.getFromConfig("config", "top-settings", "lines");
            List<Map.Entry<UUID, Integer>> topPlayers = getTopPlayers(topLines);
            List<Map.Entry<Integer, List<String>>> topRewards = configManager.getTopRewards();

            for (int i = 0; i < topLines; i++) {
                String playerName;
                int amount;

                if (i < topPlayers.size()) {
                    Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                    playerName = main.getServer().getOfflinePlayer(entry.getKey()).getName();
                    amount = entry.getValue();
                } else {
                    break;
                }

                int position = i + 1;
                for (Map.Entry<Integer, List<String>> rewardEntry : topRewards) {
                    if (rewardEntry.getKey() == position) {
                        for (String reward : rewardEntry.getValue()) {
                            String command = processReward(reward, playerName);
                            main.getServer().dispatchCommand(main.getServer().getConsoleSender(), command);
                        }
                        break;
                    }
                }
            }
        }
        else {
            SoundUtils.playSoundToAll("end-no-winner", configManager);

            List<String> noWinnerMessages = configManager.getFromConfig("event", "event", "event-end-no-winner");

            for (String message : noWinnerMessages) {
                MessageUtils.sendMessageToAll(message);
            }
        }

        playerProgress.clear();
        if (eventTimer != null) {
            eventTimer.cancel();
            eventTimer = null;
        }
        startDelayTimer();
    }

    private void spawnRandomFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type randomType = RandomUtils.getRandomElement(Arrays.asList(types));

        Color color1 = Color.fromRGB(RandomUtils.getRandomInt(0, 255), RandomUtils.getRandomInt(0, 255), RandomUtils.getRandomInt(0, 255));
        Color color2 = Color.fromRGB(RandomUtils.getRandomInt(0, 255), RandomUtils.getRandomInt(0, 255), RandomUtils.getRandomInt(0, 255));

        FireworkEffect effect = FireworkEffect.builder()
                .with(randomType)
                .withColor(color1)
                .withFade(color2)
                .flicker(RandomUtils.getRandomInt(0, 1) == 1)
                .trail(RandomUtils.getRandomInt(0, 1) == 1)
                .build();

        meta.addEffect(effect);
        meta.setPower(RandomUtils.getRandomInt(1, 2));
        firework.setFireworkMeta(meta);
    }

    private String processReward(String reward, String winnerName) {
        String command = reward.replace("%winner%", winnerName);

        String[] parts = command.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains("-")) {
                parts[i] = String.valueOf(RandomUtils.parseRandomRange(parts[i]));
            }
        }
        return String.join(" ", parts);
    }

    private long getRemainingTime(BukkitTask timer, boolean activeCondition, long startTime, long duration) {
        if (timer != null && activeCondition && !timer.isCancelled()) {
            long elapsedTicks = main.getServer().getCurrentTick() - startTime;
            long remainingTicks = duration - elapsedTicks;
            return Math.max(remainingTicks, 0L);
        }
        return 0L;
    }

    public long getDelayTimeLeft() {
        return getRemainingTime(delayTimer, !isEventActive, delayStartTime, delayDuration);
    }

    public long getEventTimeLeft() {
        return getRemainingTime(eventTimer, isEventActive, eventStartTime, eventDuration);
    }

    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        return playerProgress.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Material getTargetItem() {
        return targetItem;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void addPlayerProgress(UUID playerId, int amount) {
        playerProgress.merge(playerId, amount, Integer::sum);
    }

    public int getPlayerProgress(UUID playerId) {
        return playerProgress.getOrDefault(playerId, 0);
    }

    public HashMap<UUID, Integer> getPlayerProgress() { return playerProgress; }

    public void stopTimers() {
        if (delayTimer != null) {
            delayTimer.cancel();
            delayTimer = null;
        }
        if (eventTimer != null) {
            eventTimer.cancel();
            eventTimer = null;
            isEventActive = false;
        }
    }

    public void reloadEvent() {
        stopTimers();
        startDelayTimer();
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public String getLastWinner() {
        return lastWinner;
    }

    public void setTargetItem(Material targetItem) {
        this.targetItem = targetItem;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setCustomEventDuration(long durationTicks) {
        this.customEventDuration = durationTicks;
    }

}
