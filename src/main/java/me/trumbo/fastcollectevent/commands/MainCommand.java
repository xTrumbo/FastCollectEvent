package me.trumbo.fastcollectevent.commands;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainCommand implements CommandExecutor {

    private FastCollectEvent main;

    public MainCommand(FastCollectEvent main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        MessageUtils.FormatType format = main.getConfigManager().getCurrentFormat();

        if (args.length == 0) {
            List<String> helpMessages = new ArrayList<>();
            helpMessages.add("&e=== &6FastCollectEvent Помощь &e===");
            helpMessages.add("&a/" + label + " delay &7- Показать время до начала/конца ивента");
            helpMessages.add("&a/" + label + " top &7- Показать топ игроков");
            helpMessages.add("&a/" + label + " collect &7- Сдать предметы для ивента");
            helpMessages.add("&e======================");

            for (String message : helpMessages) {
                MessageUtils.sendMessageToPlayer(player, message, format);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("delay")) {
            long delayTicks = main.getEventManager().getDelayTimeLeft();
            long eventTicks = main.getEventManager().getEventTimeLeft();

            if (delayTicks > 0) {
                int seconds = (int) (delayTicks / 20);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String delayStart = main.getConfigManager().getFromConfig("config", "messages", "delay-start",
                        "&aДо начала ивента: &6%minutes% мин. %seconds% сек.");
                String formattedMessage = delayStart.replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
            } else if (eventTicks > 0) {
                int seconds = (int) (eventTicks / 20);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String delayEnd = main.getConfigManager().getFromConfig("config", "messages", "delay-end",
                        "&aДо конца ивента: &6%minutes% мин. %seconds% сек.");
                String formattedMessage = delayEnd.replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
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

            if (playerAmount == 0) {
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
            player.getInventory().removeItem(new ItemStack(targetItem, 0));

            main.getEventManager().addPlayerProgress(player.getUniqueId(), collectedAmount);
            int totalProgress = main.getEventManager().getPlayerProgress(player.getUniqueId());
            int remaining = targetAmount - totalProgress;

            if (totalProgress >= targetAmount) {
                main.getEventManager().endEvent(player);
            } else {
                String collected = main.getConfigManager().getFromConfig("config", "messages", "collected",
                        "&aВы сдали &6%amount% &a%item%! Осталось: &6%remaining%");
                String formattedMessage = collected.replace("%amount%", String.valueOf(collectedAmount))
                        .replace("%item%", itemTranslation)
                        .replace("%remaining%", String.valueOf(remaining));
                MessageUtils.sendMessageToPlayer(player, formattedMessage, format);
            }
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
