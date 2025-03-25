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

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            List<String> helpMessages = main.getConfigManager().getFromConfig("config", "messages", "help");
            if (helpMessages != null) {
                for (String message : helpMessages) {
                    MessageUtils.sendMessageToPlayer(player, message.replace("%label%", label));
                }
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

                String delayStart = main.getConfigManager().getFromConfig("config", "messages", "delay-start");
                if (delayStart != null) {
                    String formattedMessage = delayStart.replace("%hours%", String.valueOf(hours))
                            .replace("%minutes%", String.valueOf(minutes))
                            .replace("%seconds%", String.valueOf(seconds));
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
            } else if (eventTicks > 0) {
                int totalSeconds = (int) (eventTicks / 20);
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;
                int seconds = totalSeconds % 60;

                Material targetItem = main.getEventManager().getTargetItem();
                String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

                String delayEnd = main.getConfigManager().getFromConfig("config", "messages", "delay-end");
                if (delayEnd != null) {
                    String formattedMessage = delayEnd.replace("%hours%", String.valueOf(hours))
                            .replace("%minutes%", String.valueOf(minutes))
                            .replace("%seconds%", String.valueOf(seconds))
                            .replace("%item%", itemTranslation);
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
            } else {
                String noTimer = main.getConfigManager().getFromConfig("config", "messages", "no-timer");
                if (noTimer != null) {
                    MessageUtils.sendMessageToPlayer(player, noTimer);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event");
                if (noEvent != null) {
                    MessageUtils.sendMessageToPlayer(player, noEvent);
                }
                return true;
            }

            Integer topLines = main.getConfigManager().getFromConfig("config", "top-settings", "lines");
            String topHeader = main.getConfigManager().getFromConfig("config", "top-settings", "top-header");
            String topLineFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-line");
            String topEmptyFormat = main.getConfigManager().getFromConfig("config", "top-settings", "top-empty");

            List<Map.Entry<UUID, Integer>> topPlayers = main.getEventManager().getTopPlayers(topLines);
            Material targetItem = main.getEventManager().getTargetItem();
            String itemTranslation = main.getConfigManager().getItemTranslation(targetItem);

            if (topHeader != null) {
                MessageUtils.sendMessageToPlayer(player, topHeader);
            }

            for (int i = 0; i < topLines; i++) {
                String message;
                if (i < topPlayers.size() && topLineFormat != null) {
                    Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                    String playerName = main.getServer().getOfflinePlayer(entry.getKey()).getName();
                    if (playerName == null) playerName = "Неизвестный";

                    message = topLineFormat.replace("%position%", String.valueOf(i + 1))
                            .replace("%player%", playerName)
                            .replace("%amount%", String.valueOf(entry.getValue()))
                            .replace("%item%", itemTranslation);
                } else if (topEmptyFormat != null) {
                    message = topEmptyFormat.replace("%position%", String.valueOf(i + 1));
                } else {
                    message = String.valueOf(i + 1);
                }
                MessageUtils.sendMessageToPlayer(player, message);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("collect")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event");
                if (noEvent != null) {
                    MessageUtils.sendMessageToPlayer(player, noEvent);
                }
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
                SoundUtils.playSound(player, "no-items", main.getConfigManager());
                String noItems = main.getConfigManager().getFromConfig("config", "messages", "no-items");
                if (noItems != null) {
                    String formattedMessage = noItems.replace("%item%", itemTranslation);
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
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
                SoundUtils.playSound(player, "collect", main.getConfigManager());
                String collected = main.getConfigManager().getFromConfig("config", "messages", "collected");
                if (collected != null) {
                    String formattedMessage = collected.replace("%amount%", String.valueOf(collectedAmount))
                            .replace("%item%", itemTranslation != null ? itemTranslation : targetItem.name())
                            .replace("%remaining%", String.valueOf(remaining));
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            if (args.length != 4) {
                String usage = main.getConfigManager().getFromConfig("config", "messages", "score-usage");
                if (usage != null) {
                    MessageUtils.sendMessageToPlayer(player, usage.replace("%label%", label));
                }
                return true;
            }

            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getConfigManager().getFromConfig("config", "messages", "no-event");
                if (noEvent != null) {
                    MessageUtils.sendMessageToPlayer(player, noEvent);
                }
                return true;
            }

            String operation = args[1].toLowerCase();
            String targetPlayerName = args[2];
            int amount;

            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                String invalidAmount = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-amount");
                if (invalidAmount != null) {
                    MessageUtils.sendMessageToPlayer(player, invalidAmount);
                }
                return true;
            }

            Player targetPlayer = main.getServer().getPlayerExact(targetPlayerName);
            UUID targetUUID = (targetPlayer != null) ? targetPlayer.getUniqueId() :
                    main.getServer().getOfflinePlayer(targetPlayerName).getUniqueId();

            if (!operation.equals("plus") && !operation.equals("minus")) {
                String invalidOp = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-operation");
                if (invalidOp != null) {
                    MessageUtils.sendMessageToPlayer(player, invalidOp);
                }
                return true;
            }

            int currentProgress = main.getEventManager().getPlayerProgress(targetUUID);
            int newProgress;

            if (operation.equals("plus")) {
                main.getEventManager().addPlayerProgress(targetUUID, amount);
                newProgress = currentProgress + amount;
                String scorePlus = main.getConfigManager().getFromConfig("config", "messages", "score-plus");
                if (scorePlus != null) {
                    String formattedMessage = scorePlus.replace("%amount%", String.valueOf(amount))
                            .replace("%player%", targetPlayerName)
                            .replace("%newscore%", String.valueOf(newProgress));
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
            } else {
                newProgress = Math.max(0, currentProgress - amount);
                main.getEventManager().addPlayerProgress(targetUUID, newProgress - currentProgress);
                String scoreMinus = main.getConfigManager().getFromConfig("config", "messages", "score-minus");
                if (scoreMinus != null) {
                    String formattedMessage = scoreMinus.replace("%amount%", String.valueOf(amount))
                            .replace("%player%", targetPlayerName)
                            .replace("%newscore%", String.valueOf(newProgress));
                    MessageUtils.sendMessageToPlayer(player, formattedMessage);
                }
            }

            if (newProgress >= main.getEventManager().getTargetAmount() && targetPlayer != null) {
                main.getEventManager().endEvent(targetPlayer);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start") && sender.hasPermission("fce.admin")) {
            if (main.getEventManager().isEventActive()) {
                String alreadyActive = main.getConfigManager().getFromConfig("config", "messages", "already-active");
                if (alreadyActive != null) {
                    MessageUtils.sendMessageToPlayer(player, alreadyActive);
                }
                return true;
            }

            main.getEventManager().stopTimers();

            if (args.length >= 3) {
                try {
                    Material targetItem = Material.matchMaterial(args[1].toUpperCase());
                    if (targetItem == null) {
                        String invalidItem = main.getConfigManager().getFromConfig("config", "messages", "invalid-item");
                        if (invalidItem != null) {
                            MessageUtils.sendMessageToPlayer(player, invalidItem);
                        }
                        return true;
                    }

                    int targetAmount = Integer.parseInt(args[2]);
                    if (targetAmount <= 0) {
                        String invalidAmount = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-amount");
                        if (invalidAmount != null) {
                            MessageUtils.sendMessageToPlayer(player, invalidAmount);
                        }
                        return true;
                    }

                    main.getEventManager().setTargetItem(targetItem);
                    main.getEventManager().setTargetAmount(targetAmount);
                } catch (NumberFormatException e) {
                    String invalidAmount = main.getConfigManager().getFromConfig("config", "messages", "score-invalid-amount");
                    if (invalidAmount != null) {
                        MessageUtils.sendMessageToPlayer(player, invalidAmount);
                    }
                    return true;
                }
            }

            main.getEventManager().startEventTimer();

            String started = main.getConfigManager().getFromConfig("config", "messages", "event-started");
            if (started != null) {
                MessageUtils.sendMessageToPlayer(player, started);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("fce.admin")) {
            if (!main.getEventManager().isEventActive()) {
                String notActive = main.getConfigManager().getFromConfig("config", "messages", "not-active");
                if (notActive != null) {
                    MessageUtils.sendMessageToPlayer(player, notActive);
                }
                return true;
            }

            main.getEventManager().endEvent(null);

            String stopped = main.getConfigManager().getFromConfig("config", "messages", "event-stopped");
            if (stopped != null) {
                MessageUtils.sendMessageToPlayer(player, stopped);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("fce.admin")) {
            main.getConfigManager().createFiles();
            main.getEventManager().reloadEvent();
            String reloadMessage = main.getConfigManager().getFromConfig("config", "messages", "reload");
            if (reloadMessage != null) {
                MessageUtils.sendMessageToPlayer(player, reloadMessage);
            }
        } else {
            String noPerm = main.getConfigManager().getFromConfig("config", "messages", "no-perm");
            if (noPerm != null) {
                MessageUtils.sendMessageToPlayer(player, noPerm);
            }
        }
        return true;
    }
}
