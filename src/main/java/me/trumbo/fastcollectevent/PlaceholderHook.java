package me.trumbo.fastcollectevent;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
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
        return "1.3";
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
            try {
                int position = Integer.parseInt(identifier.replace("top_name_", ""));
                return position <= topNames.size() ? topNames.get(position - 1) : "N/A";
            } catch (NumberFormatException ignored) {
                return "";
            }
        } else if (identifier.startsWith("top_score_")) {
            try {
                int position = Integer.parseInt(identifier.replace("top_score_", ""));
                return position <= topScores.size() ? String.valueOf(topScores.get(position - 1)) : "0";
            } catch (NumberFormatException ignored) {
                return "";
            }


        } else if (identifier.equals("time")) {
            long delayTicks = main.getEventManager().getDelayTimeLeft();
            long eventTicks = main.getEventManager().getEventTimeLeft();

            if (delayTicks > 0) {
                int totalSeconds = (int) (delayTicks / 20);
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int seconds = totalSeconds % 60;

                String delayStart = main.getConfigManager().getFromConfig("config", "messages", "delay-start",
                        "&aДо начала ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
                return delayStart.replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
            } else if (eventTicks > 0) {
                int totalSeconds = (int) (eventTicks / 20);
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int seconds = totalSeconds % 60;

                String delayEnd = main.getConfigManager().getFromConfig("config", "messages", "delay-end",
                        "&aДо конца ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
                return delayEnd.replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
            }
        }
        return null;
    }
}
