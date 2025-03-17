package me.trumbo.fastcollectevent.managers;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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

    public EventManager(FastCollectEvent main) {
        this.isEventActive = false;
        this.main = main;
        this.playerProgress = new HashMap<>();

        startDelayTimer();
    }

    private void startDelayTimer() {
        Object[] itemData = main.getConfigManager().getRandomEventItem();
        this.targetItem = (Material) itemData[0];
        this.targetAmount = (int) itemData[1];

        int delaySeconds = main.getConfigManager().getFromConfig("event", "event", "start-delay", 8);
        delayDuration = delaySeconds * 20L;

        delayTimer = main.getServer().getScheduler().runTaskLater(main, () -> {
            startEventTimer();
        }, delayDuration);
        delayStartTime = main.getServer().getCurrentTick();
    }

    private void startEventTimer() {
        ConfigManager configManager = main.getConfigManager();
        int eventDurationSeconds = configManager.getFromConfig("event", "event", "duration", 12);
        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

        eventDuration = eventDurationSeconds * 20L;

        isEventActive = true;

        String soundName = configManager.getFromConfig("event", "event", "start-sound", null);
        if (soundName != null) {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                for (Player player : main.getServer().getOnlinePlayers()) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
        }

        List<String> startMessages = configManager.getFromConfig("event", "event", "start-message",
                Arrays.asList("&aИвент начался! Соберите " + targetAmount + " " + itemTranslation + "!"));
        MessageUtils.FormatType format = configManager.getCurrentFormat();
        for (String message : startMessages) {
            String formattedMessage = message.replace("%item%", itemTranslation)
                    .replace("%int%", String.valueOf(targetAmount));
            MessageUtils.sendMessageToAll(formattedMessage, format);
        }

        eventTimer = main.getServer().getScheduler().runTaskLater(main, () -> {
            endEvent(null);
        }, eventDuration);
        eventStartTime = main.getServer().getCurrentTick();
    }

    public void endEvent(Player winner) {
        isEventActive = false;
        ConfigManager configManager = main.getConfigManager();
        MessageUtils.FormatType format = configManager.getCurrentFormat();
        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

        String winnerName = null;
        int winnerProgress = 0;

        if (winner != null) {
            winnerName = winner.getName();
            winnerProgress = playerProgress.get(winner.getUniqueId());
        } else if (!playerProgress.isEmpty()) {
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
        }

        if (winnerName != null) {
            List<String> endMessages = configManager.getFromConfig("event", "event", "event-end",
                    Arrays.asList("&cИвент завершён! Победитель: &6%winner% &7собрал &6%amount% &7из &6%int%"));
            for (String message : endMessages) {
                String formattedMessage = message.replace("%item%", itemTranslation)
                        .replace("%int%", String.valueOf(targetAmount))
                        .replace("%winner%", winnerName)
                        .replace("%amount%", String.valueOf(winnerProgress));
                MessageUtils.sendMessageToAll(formattedMessage, format);
            }

            List<String> rewards = configManager.getFromConfig("event", "event", "rewards",
                    Arrays.asList("give %winner% diamond 64"));
            for (String reward : rewards) {
                String command = reward.replace("%winner%", winnerName);
                main.getServer().dispatchCommand(main.getServer().getConsoleSender(), command);
            }
        } else {
            List<String> noWinnerMessages = configManager.getFromConfig("event", "event", "event-end-no-winner",
                    Arrays.asList("&cИвент завершён! Никто не участвовал."));
            for (String message : noWinnerMessages) {
                MessageUtils.sendMessageToAll(message, format);
            }
        }

        playerProgress.clear();
        if (eventTimer != null) {
            eventTimer.cancel();
            eventTimer = null;
        }
        startDelayTimer();
    }

    public long getDelayTimeLeft() {
        if (delayTimer != null && !isEventActive && !delayTimer.isCancelled()) {
            long elapsedTicks = main.getServer().getCurrentTick() - delayStartTime;
            long remainingTicks = delayDuration - elapsedTicks;
            return Math.max(remainingTicks, 0L);
        }
        return 0L;
    }

    public long getEventTimeLeft() {
        if (eventTimer != null && isEventActive && !eventTimer.isCancelled()) {
            long elapsedTicks = main.getServer().getCurrentTick() - eventStartTime;
            long remainingTicks = eventDuration - elapsedTicks;
            return Math.max(remainingTicks, 0L);
        }
        return 0L;
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

}
