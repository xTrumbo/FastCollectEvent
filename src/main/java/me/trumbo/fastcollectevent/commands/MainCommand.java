package me.trumbo.fastcollectevent.commands;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import me.trumbo.fastcollectevent.utils.SoundUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MainCommand implements CommandExecutor {

    private final FastCollectEvent main;

    public MainCommand(FastCollectEvent main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return handleHelp(player, label);
        }
        if (args[0].equalsIgnoreCase("delay")) {
            return handleDelay(player);
        }
        if (args[0].equalsIgnoreCase("top")) {
            return handleTop(player);
        }
        if (args[0].equalsIgnoreCase("collect")) {
            return handleCollect(player);
        }
        if (args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            return handleScore(player, args, label);
        }
        if (args[0].equalsIgnoreCase("start") && sender.hasPermission("fce.admin")) {
            return handleStart(player);
        }
        if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("fce.admin")) {
            return handleStop(player);
        }
        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("fce.admin")) {
            return handleReload(player);
        }

        sendNoPermissionMessage(player);
        return true;
    }

    private boolean handleHelp(Player player, String label) {
        List<String> helpMessages = main.getConfigManager().getFromConfig("config", "messages", "help",
                Arrays.asList("&e=== &6FastCollectEvent Помощь &e===",
                        "&a/%label% delay &7- Показать время до начала/конца ивента",
                        "&a/%label% top &7- Показать топ игроков",
                        "&a/%label% collect &7- Сдать предметы для ивента",
                        "&e======================"));
        for (String message : helpMessages) {
            MessageUtils.sendMessageToPlayer(player, message.replace("%label%", label));
        }
        return true;
    }

    private boolean handleDelay(Player player) {
        long delayTicks = main.getEventManager().getDelayTimeLeft();
        long eventTicks = main.getEventManager().getEventTimeLeft();

        if (delayTicks > 0) {
            sendTimeMessage(player, delayTicks, "delay-start", "&aДо начала ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
        } else if (eventTicks > 0) {
            sendTimeMessage(player, eventTicks, "delay-end", "&aДо конца ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
        } else {
            sendConfigMessage(player, "no-timer", "&cСейчас нет активного таймера!");
        }
        return true;
    }

    private boolean handleTop(Player player) {
        if (!main.getEventManager().isEventActive()) {
            sendConfigMessage(player, "no-event", "&cСейчас нет активного ивента!");
            return true;
        }

        int topLines = main.getConfigManager().getFromConfig("config", "top-settings", "lines", 10);
        String topHeader = main.getConfigManager().getFromConfig("config", "top-settings", "top-header", "&aТоп игроков по ивенту:");
        String topLineFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-line", "&e%position%. &6%player% - &a%amount% %item%");
        String topEmptyFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-empty", "&e%position%. &7N/A");

        List<Map.Entry<UUID, Integer>> topPlayers = main.getEventManager().getTopPlayers(topLines);
        Material targetItem = main.getEventManager().getTargetItem();
        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

        MessageUtils.sendMessageToPlayer(player, topHeader);

        for (int i = 0; i < topLines; i++) {
            String message = (i < topPlayers.size())
                    ? formatTopPlayer(topLineFormat, i + 1, topPlayers.get(i), itemTranslation)
                    : topEmptyFormat.replace("%position%", String.valueOf(i + 1));
            MessageUtils.sendMessageToPlayer(player, message);
        }
        return true;
    }

    private boolean handleCollect(Player player) {
        if (!main.getEventManager().isEventActive()) {
            sendConfigMessage(player, "no-event", "&cСейчас нет активного ивента!");
            return true;
        }

        Material targetItem = main.getEventManager().getTargetItem();
        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);
        int targetAmount = main.getEventManager().getTargetAmount();

        int collectedAmount = collectItems(player, targetItem);
        if (collectedAmount == 0) {
            SoundUtils.playSound(player, "no-items", main.getConfigManager());
            sendConfigMessage(player, "no-items", "&cУ вас нет &6%item%!", "%item%", itemTranslation);
            return true;
        }

        main.getDatabaseManager().addOrUpdatePlayer(player.getName(), collectedAmount);
        main.getEventManager().addPlayerProgress(player.getUniqueId(), collectedAmount);
        int totalProgress = main.getEventManager().getPlayerProgress(player.getUniqueId());
        int remaining = targetAmount - totalProgress;

        if (totalProgress >= targetAmount) {
            main.getEventManager().endEvent(player);
        } else {
            SoundUtils.playSound(player, "collect", main.getConfigManager());
            String message = main.getConfigManager().getFromConfig("config", "messages", "collected",
                            "&aВы сдали &6%amount% &a%item%! Осталось: &6%remaining%")
                    .replace("%amount%", String.valueOf(collectedAmount))
                    .replace("%item%", itemTranslation)
                    .replace("%remaining%", String.valueOf(remaining));
            MessageUtils.sendMessageToPlayer(player, message);
        }
        return true;
    }

    private boolean handleScore(Player player, String[] args, String label) {
        if (args.length != 4) {
            sendConfigMessage(player, "score-usage", "&cИспользование: /%label% score <plus/minus> <игрок> <число>", "%label%", label);
            return true;
        }
        if (!main.getEventManager().isEventActive()) {
            sendConfigMessage(player, "no-event", "&cСейчас нет активного ивента!");
            return true;
        }

        String operation = args[1].toLowerCase();
        String targetPlayerName = args[2];
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendConfigMessage(player, "score-invalid-amount", "&cКоличество должно быть положительным числом!");
            return true;
        }

        Player targetPlayer = main.getServer().getPlayerExact(targetPlayerName);
        UUID targetUUID = (targetPlayer != null) ? targetPlayer.getUniqueId() : main.getServer().getOfflinePlayer(targetPlayerName).getUniqueId();

        if (!operation.equals("plus") && !operation.equals("minus")) {
            sendConfigMessage(player, "score-invalid-operation", "&cДолжно быть 'plus' или 'minus'!");
            return true;
        }

        int currentProgress = main.getEventManager().getPlayerProgress(targetUUID);
        int newProgress = operation.equals("plus") ? currentProgress + amount : Math.max(0, currentProgress - amount);
        main.getEventManager().addPlayerProgress(targetUUID, newProgress - currentProgress);

        String configKey = operation.equals("plus") ? "score-plus" : "score-minus";
        String defaultMessage = operation.equals("plus")
                ? "&aДобавлено &6%amount%&a очков игроку &6%player%&a. Новый счёт: &6%newscore%"
                : "&cУбрано &6%amount%&c очков у игрока &6%player%&c. Новый счёт: &6%newscore%";
        String message = main.getConfigManager().getFromConfig("config", "messages", configKey, defaultMessage)
                .replace("%amount%", String.valueOf(amount))
                .replace("%player%", targetPlayerName)
                .replace("%newscore%", String.valueOf(newProgress));
        MessageUtils.sendMessageToPlayer(player, message);

        if (newProgress >= main.getEventManager().getTargetAmount() && targetPlayer != null) {
            main.getEventManager().endEvent(targetPlayer);
        }
        return true;
    }

    private boolean handleStart(Player player) {
        if (main.getEventManager().isEventActive()) {
            sendConfigMessage(player, "already-active", "&cИвент уже активен!");
            return true;
        }

        main.getEventManager().stopTimers();
        main.getEventManager().startEventTimer();
        sendConfigMessage(player, "event-started", "&aИвент успешно запущен!");
        return true;
    }

    private boolean handleStop(Player player) {
        if (!main.getEventManager().isEventActive()) {
            sendConfigMessage(player, "not-active", "&cИвент сейчас не активен!");
            return true;
        }

        main.getEventManager().endEvent(null);
        sendConfigMessage(player, "event-stopped", "&aИвент успешно остановлен!");
        return true;
    }

    private boolean handleReload(Player player) {
        main.getConfigManager().createFiles();
        main.getEventManager().reloadEvent();
        sendConfigMessage(player, "reload", "&aКонфигурация перезагружена!");
        return true;
    }

    private void sendTimeMessage(Player player, long ticks, String configKey, String defaultMessage) {
        int totalSeconds = (int) (ticks / 20);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        Material targetItem = main.getEventManager().getTargetItem();
        String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

        String message = main.getConfigManager().getFromConfig("config", "messages", configKey, defaultMessage)
                .replace("%hours%", String.valueOf(hours))
                .replace("%minutes%", String.valueOf(minutes))
                .replace("%seconds%", String.valueOf(seconds))
                .replace("%item%", itemTranslation != null ? itemTranslation : targetItem.name());

        MessageUtils.sendMessageToPlayer(player, message);
    }

    private void sendConfigMessage(Player player, String configKey, String defaultMessage, String... replacements) {
        String message = main.getConfigManager().getFromConfig("config", "messages", configKey, defaultMessage);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        MessageUtils.sendMessageToPlayer(player, message);
    }

    private void sendNoPermissionMessage(Player player) {
        sendConfigMessage(player, "no-perm", "&cУ вас нет прав!");
    }

    private String formatTopPlayer(String format, int position, Map.Entry<UUID, Integer> entry, String itemTranslation) {
        String playerName = main.getServer().getOfflinePlayer(entry.getKey()).getName();
        if (playerName == null) playerName = "Неизвестный";
        return format.replace("%position%", String.valueOf(position))
                .replace("%player%", playerName)
                .replace("%amount%", String.valueOf(entry.getValue()))
                .replace("%item%", itemTranslation);
    }

    private int collectItems(Player player, Material targetItem) {
        int collectedAmount = 0;
        for (ItemStack stack : player.getInventory().all(targetItem).values()) {
            collectedAmount += stack.getAmount();
            stack.setAmount(0);
        }
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem != null && offHandItem.getType() == targetItem) {
            collectedAmount += offHandItem.getAmount();
            player.getInventory().setItemInOffHand(null);
        }
        return collectedAmount;
    }
}