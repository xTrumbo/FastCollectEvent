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

    private FastCollectEvent main;

    public MainCommand(FastCollectEvent main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        MessageUtils.FormatType format = main.getConfigManager().getCurrentFormat();

        if (args.length == 0 || (args[0].equalsIgnoreCase("help"))) {
            List<String> helpMessages = main.getConfigManager().getFromConfig("config", "messages", "help",
                    Arrays.asList("&e=== &6FastCollectEvent Помощь &e===",
                            "&a/%label% delay &7- Показать время до начала/конца ивента",
                            "&a/%label% top &7- Показать топ игроков",
                            "&a/%label% collect &7- Сдать предметы для ивента",
                            "&e======================"));
            for (String message : helpMessages) {
                MessageUtils.sendMessageToPlayer(player, message.replace("%label%", label), format);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("delay")) {
            long delayTicks = main.getEventManager().getDelayTimeLeft();
            long eventTicks = main.getEventManager().getEventTimeLeft();

            if (delayTicks > 0) {
                int totalSeconds = (int) (delayTicks / 20);
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int seconds = totalSeconds % 60;

                String delayStart = main.getConfigManager().getFromConfig("config", "messages", "delay-start",
                        "&aДо начала ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
                String formattedMessage = delayStart.replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
            } else if (eventTicks > 0) {
                int totalSeconds = (int) (eventTicks / 20);
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int seconds = totalSeconds % 60;

                String delayEnd = main.getConfigManager().getFromConfig("config", "messages", "delay-end",
                        "&aДо конца ивента: &6%hours% ч. %minutes% мин. %seconds% сек.");
                String formattedMessage = delayEnd.replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
            } else {
                String noTimer = main.getConfigManager().getFromConfig("config", "messages", "no-timer",
                        "&cСейчас нет активного таймера!");
                MessageUtils.sendMessageToPlayer(player, noTimer, format);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event",
                        "&cСейчас нет активного ивента!");
                MessageUtils.sendMessageToPlayer(player, noEvent, format);
                return true;
            }

            int topLines = main.getConfigManager().getFromConfig("config", "top-settings", "lines", 10);
            String topHeader = main.getConfigManager().getFromConfig("config", "top-settings", "top-header",
                    "&aТоп игроков по ивенту:");
            String topLineFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-line",
                    "&e%position%. &6%player% - &a%amount% %item%");
            String topEmptyFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-empty",
                    "&e%position%. &7N/A");

            List<Map.Entry<UUID, Integer>> topPlayers = main.getEventManager().getTopPlayers(topLines);
            Material targetItem = main.getEventManager().getTargetItem();
            String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

            MessageUtils.sendMessageToPlayer(player, topHeader, format);

            for (int i = 0; i < topLines; i++) {
                String message;
                if (i < topPlayers.size()) {
                    Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                    String playerName = main.getServer().getOfflinePlayer(entry.getKey()).getName();
                    if (playerName == null) playerName = "Неизвестный";

                    message = topLineFormat.replace("%position%", String.valueOf(i + 1))
                            .replace("%player%", playerName)
                            .replace("%amount%", String.valueOf(entry.getValue()))
                            .replace("%item%", itemTranslation);
                } else {
                    message = topEmptyFormat.replace("%position%", String.valueOf(i + 1));
                }
                MessageUtils.sendMessageToPlayer(player, message, format);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("collect")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event",
                        "&cСейчас нет активного ивента!");
                MessageUtils.sendMessageToPlayer(player, noEvent, format);
                return true;
            }

            Material targetItem = main.getEventManager().getTargetItem();
            String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);
            int targetAmount = main.getEventManager().getTargetAmount();
            int playerAmount = player.getInventory().all(targetItem).values().stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();

            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (offHandItem != null && offHandItem.getType() == targetItem) {
                playerAmount += offHandItem.getAmount();
            }

            if (playerAmount == 0) {

                SoundUtils.playSound(player,"no-items", main.getConfigManager());

                String noItems = main.getConfigManager().getFromConfig("config", "messages", "no-items",
                        "&cУ вас нет &6%item%!");
                String formattedMessage = noItems.replace("%item%", itemTranslation);
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
                return true;
            }

            int collectedAmount = 0;
            for (ItemStack stack : player.getInventory().all(targetItem).values()) {
                collectedAmount += stack.getAmount();
                stack.setAmount(0);
            }

            if (offHandItem != null && offHandItem.getType() == targetItem) {
                collectedAmount += offHandItem.getAmount();
                player.getInventory().setItemInOffHand(null);
            }

            main.getDatabaseManager().addOrUpdatePlayer(player.getName(), collectedAmount);
            player.getInventory().removeItem(new ItemStack(targetItem, 0));

            main.getEventManager().addPlayerProgress(player.getUniqueId(), collectedAmount);
            int totalProgress = main.getEventManager().getPlayerProgress(player.getUniqueId());
            int remaining = targetAmount - totalProgress;

            if (totalProgress >= targetAmount) {
                main.getEventManager().endEvent(player);
            } else {

                SoundUtils.playSound(player,"collect", main.getConfigManager());

                String collected = main.getConfigManager().getFromConfig("config", "messages", "collected",
                        "&aВы сдали &6%amount% &a%item%! Осталось: &6%remaining%");
                String formattedMessage = collected.replace("%amount%", String.valueOf(collectedAmount))
                        .replace("%item%", itemTranslation)
                        .replace("%remaining%", String.valueOf(remaining));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            if (args.length != 4) {
                String usage = main.getConfigManager().getFromConfig("config", "messages", "score-usage",
                        "&cИспользование: /%label% score <plus/minus> <игрок> <число>").replace("%label%", label);
                MessageUtils.sendMessageToPlayer(player, usage, format);
                return true;
            }

            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event",
                        "&cСейчас нет активного ивента!");
                MessageUtils.sendMessageToPlayer(player, noEvent, format);
                return true;
            }

            String operation = args[1].toLowerCase();
            String targetPlayerName = args[2];
            int amount;

            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                String invalidAmount = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-amount",
                        "&cКоличество должно быть положительным числом!");
                MessageUtils.sendMessageToPlayer(player, invalidAmount, format);
                return true;
            }

            Player targetPlayer = main.getServer().getPlayerExact(targetPlayerName);
            UUID targetUUID = (targetPlayer != null) ? targetPlayer.getUniqueId() :
                    main.getServer().getOfflinePlayer(targetPlayerName).getUniqueId();

            if (!operation.equals("plus") && !operation.equals("minus")) {
                String invalidOp = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-operation",
                        "&cДолжно быть 'plus' или 'minus'!");
                MessageUtils.sendMessageToPlayer(player, invalidOp, format);
                return true;
            }

            int currentProgress = main.getEventManager().getPlayerProgress(targetUUID);
            int newProgress;

            if (operation.equals("plus")) {
                main.getEventManager().addPlayerProgress(targetUUID, amount);
                newProgress = currentProgress + amount;
                String scorePlus = main.getConfigManager().getFromConfig("config", "messages", "score-plus",
                                "&aДобавлено &6%amount%&a очков игроку &6%player%&a. Новый счёт: &6%newscore%")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%player%", targetPlayerName)
                        .replace("%newscore%", String.valueOf(newProgress));
                MessageUtils.sendMessageToPlayer(player, scorePlus, format);
            } else {
                newProgress = Math.max(0, currentProgress - amount);
                main.getEventManager().addPlayerProgress(targetUUID, newProgress - currentProgress);
                String scoreMinus = main.getConfigManager().getFromConfig("config", "messages", "score-minus",
                                "&cУбрано &6%amount%&c очков у игрока &6%player%&c. Новый счёт: &6%newscore%")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%player%", targetPlayerName)
                        .replace("%newscore%", String.valueOf(newProgress));
                MessageUtils.sendMessageToPlayer(player, scoreMinus, format);
            }

            if (newProgress >= main.getEventManager().getTargetAmount() && targetPlayer != null) {
                main.getEventManager().endEvent(targetPlayer);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start") && sender.hasPermission("fce.admin")) {
            if (main.getEventManager().isEventActive()) {
                String alreadyActive = main.getConfigManager().getFromConfig("config", "messages", "already-active",
                        "&cИвент уже активен!");
                MessageUtils.sendMessageToPlayer(player, alreadyActive, format);
                return true;
            }

            main.getEventManager().stopTimers();

            main.getEventManager().startEventTimer();

            String started = main.getConfigManager().getFromConfig("config", "messages", "event-started",
                    "&aИвент успешно запущен!");
            MessageUtils.sendMessageToPlayer(player, started, format);
            return true;
        }

        if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("fce.admin")) {
            if (!main.getEventManager().isEventActive()) {
                String notActive = main.getConfigManager().getFromConfig("config", "messages", "not-active",
                        "&cИвент сейчас не активен!");
                MessageUtils.sendMessageToPlayer(player, notActive, format);
                return true;
            }

            main.getEventManager().endEvent(null);

            String stopped = main.getConfigManager().getFromConfig("config", "messages", "event-stopped",
                    "&aИвент успешно остановлен!");
            MessageUtils.sendMessageToPlayer(player, stopped, format);
            return true;
        }

        if (sender.hasPermission("fce.admin") && args[0].equalsIgnoreCase("reload")) {
            main.getConfigManager().createFiles();
            main.getEventManager().reloadEvent();
            String reloadMessage = main.getConfigManager().getFromConfig("config", "messages", "reload",
                    "&aКонфигурация перезагружена!");
            MessageUtils.sendMessageToPlayer(player, reloadMessage, format);
        } else {
            String noPerm = main.getConfigManager().getFromConfig("config", "messages", "no-perm",
                    "&cУ вас нет прав!");
            MessageUtils.sendMessageToPlayer(player, noPerm, format);
        }
        return true;
    }
}
