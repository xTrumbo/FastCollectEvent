package me.trumbo.fastcollectevent;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {

    private final FastCollectEvent main;
    private List<String> topNames = new ArrayList<>();
    private List<Integer> topScores = new ArrayList<>();

    public PlaceholderHook(FastCollectEvent main) {
        this.main = main;
        if (main.getDatabaseManager().isEnabled()) {
            updateTop();
            main.getServer().getScheduler().runTaskTimerAsynchronously(main, this::updateTop, 0L, 600L);
        }
    }

    @Override
    public String getIdentifier() {
        return "fce";
    }

    @Override
    public String getAuthor() {
        return "Trumbo";
    }

    @Override
    public String getVersion() {
        return "1.7";
    }

    @Override
    public boolean persist() {
        return true;
    }

    private void updateTop() {
        main.getDatabaseManager().getTopPlayers(10).thenAccept(top -> {
            topNames = top[0];
            topScores = top[1];
        });
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.startsWith("top_name_")) {
                int position = Integer.parseInt(identifier.replace("top_name_", ""));
                return position <= topNames.size() ? topNames.get(position - 1) : "N/A";
        } else if (identifier.startsWith("top_score_")) {
                int position = Integer.parseInt(identifier.replace("top_score_", ""));
                return position <= topScores.size() ? String.valueOf(topScores.get(position - 1)) : "0";
        } else if (identifier.equals("time")) {
            long delayTicks = main.getEventManager().getDelayTimeLeft();
            long eventTicks = main.getEventManager().getEventTimeLeft();

            if (delayTicks > 0) {
                int totalSeconds = (int) (delayTicks / 20);
                return formatTime(totalSeconds);
            } else if (eventTicks > 0) {
                int totalSeconds = (int) (eventTicks / 20);
                return formatTime(totalSeconds);
            } else {
                return "0 ч 00 мин. 00 сек.";
            }
        } else if (identifier.equals("item_id")) {
            Material targetItem = main.getEventManager().getTargetItem();
            return targetItem != null ? targetItem.name() : "N/A";
        } else if (identifier.equals("item_name")) {
            Material targetItem = main.getEventManager().getTargetItem();
            if (targetItem != null) {
                String translation = main.getConfigManager().getItemTranslation(targetItem);
                return translation != null ? translation : targetItem.name();
            }
            return "N/A";
        } else if (identifier.equals("event_status")) {
            return main.getEventManager().isEventActive() ? "Активен" : "Неактивен";
        } else if (identifier.equals("last_winner")) {
            String lastWinner = main.getEventManager().getLastWinner();
            return lastWinner != null ? lastWinner : "Никто";
        }
        return null;
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d ч %02d мин. %02d сек.", hours, minutes, seconds);
    }
}
