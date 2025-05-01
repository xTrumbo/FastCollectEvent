package me.trumbo.fastcollectevent.commands;

import me.trumbo.fastcollectevent.FastCollectEvent;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import me.trumbo.fastcollectevent.utils.RandomUtils;
import me.trumbo.fastcollectevent.utils.SoundUtils;
import me.trumbo.fastcollectevent.utils.TimeUtils;
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
        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                for (String message : main.getPluginConfig().getConfigData().getHelpMessages()) {
                    MessageUtils.sendMessage(sender, message.replace("%label%", label));
                }
                return true;
            }

        if (args[0].equalsIgnoreCase("delay")) {
            long delayTicks = main.getEventManager().getDelayTimeLeft();
            long eventTicks = main.getEventManager().getEventTimeLeft();

            if (delayTicks > 0) {
                TimeUtils.TimeRemaining time = TimeUtils.ticksToTime(delayTicks);
                String delayStart = main.getPluginConfig().getConfigData().getDelayStartMessage();
                if (delayStart != null) {
                    String formattedMessage = TimeUtils.formatTime(delayStart, time);
                    MessageUtils.sendMessage(sender, formattedMessage);
                }
            } else if (eventTicks > 0) {
                TimeUtils.TimeRemaining time = TimeUtils.ticksToTime(eventTicks);
                Material targetItem = main.getEventManager().getTargetItem();
                String itemTranslation = main.getPluginConfig().getTranslationData().getTranslation(targetItem);
                String delayEnd = main.getPluginConfig().getConfigData().getDelayEndMessage();
                if (delayEnd != null) {
                    String formattedMessage = TimeUtils.formatTime(delayEnd, time, "%item%", itemTranslation);
                    MessageUtils.sendMessage(sender, formattedMessage);
                }
            } else {
                String noTimer = main.getPluginConfig().getConfigData().getNoEventMessage();
                if (noTimer != null) {
                    MessageUtils.sendMessage(sender, noTimer);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getPluginConfig().getConfigData().getNoEventMessage();
                if (noEvent != null) {
                    MessageUtils.sendMessage(sender, noEvent);
                }
                return true;
            }

            Integer topLines = main.getPluginConfig().getConfigData().getTopLines();
            String topHeader = main.getPluginConfig().getConfigData().getTopHeader();
            String topLineFormat = main.getPluginConfig().getConfigData().getTopLine();
            String topEmptyFormat = main.getPluginConfig().getConfigData().getTopEmpty();

            List<Map.Entry<UUID, Integer>> topPlayers = main.getEventManager().getTopPlayers(topLines);
            Material targetItem = main.getEventManager().getTargetItem();
            String itemTranslation = main.getPluginConfig().getTranslationData().getTranslation(targetItem);

            if (topHeader != null) {
                MessageUtils.sendMessage(sender, topHeader);
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
                MessageUtils.sendMessage(sender, message);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("collect")) {

            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getPluginConfig().getConfigData().getNoEventMessage();
                if (noEvent != null) {
                    MessageUtils.sendMessage(sender, noEvent);
                }
                return true;
            }

            Material targetItem = main.getEventManager().getTargetItem();
            String itemTranslation = main.getPluginConfig().getTranslationData().getTranslation(targetItem);
            int targetAmount = main.getEventManager().getTargetAmount();
            int playerAmount = player.getInventory().all(targetItem).values().stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();

            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (offHandItem != null && offHandItem.getType() == targetItem) {
                playerAmount += offHandItem.getAmount();
            }

            if (playerAmount == 0) {
                SoundUtils.playSound(main, player, "no-items");
                String noItems = main.getPluginConfig().getConfigData().getNoItemsMessage();
                if (noItems != null) {
                    String formattedMessage = noItems.replace("%item%", itemTranslation);
                    MessageUtils.sendMessage(sender, formattedMessage);
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

            int halfTarget = targetAmount / 2;
            if (totalProgress >= halfTarget && (totalProgress - collectedAmount) < halfTarget) {
                List<String> halfMessages = main.getPluginConfig().getEventData().getHalfReached();
                SoundUtils.playSoundToAll(main, "half-reached");
                if (halfMessages != null) {
                    for (String message : halfMessages) {
                        String formattedMessage = message
                                .replace("%player%", player.getName())
                                .replace("%amount%", String.valueOf(totalProgress))
                                .replace("%item%", itemTranslation != null ? itemTranslation : targetItem.name())
                                .replace("%target%", String.valueOf(targetAmount));
                        MessageUtils.sendMessageToAll(formattedMessage);
                    }
                }
            }

            if (totalProgress >= targetAmount) {
                main.getEventManager().endEvent(player);
            } else {
                SoundUtils.playSound(main, player, "collect");
                String collected = main.getPluginConfig().getConfigData().getCollectedMessage();
                if (collected != null) {
                    String formattedMessage = collected.replace("%amount%", String.valueOf(collectedAmount))
                            .replace("%item%", itemTranslation != null ? itemTranslation : targetItem.name())
                            .replace("%remaining%", String.valueOf(remaining));
                    MessageUtils.sendMessage(sender, formattedMessage);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            if (args.length != 4) {
                String usage = main.getPluginConfig().getConfigData().getScoreUsageMessage();
                if (usage != null) {
                    MessageUtils.sendMessage(sender, usage.replace("%label%", label));
                }
                return true;
            }

            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getPluginConfig().getConfigData().getNoEventMessage();
                if (noEvent != null) {
                    MessageUtils.sendMessage(sender, noEvent);
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
                String invalidAmount = main.getPluginConfig().getConfigData().getScoreInvalidAmountMessage();
                if (invalidAmount != null) {
                    MessageUtils.sendMessage(sender, invalidAmount);
                }
                return true;
            }

            Player targetPlayer = main.getServer().getPlayerExact(targetPlayerName);
            UUID targetUUID = (targetPlayer != null) ? targetPlayer.getUniqueId() :
                    main.getServer().getOfflinePlayer(targetPlayerName).getUniqueId();

            if (!operation.equals("plus") && !operation.equals("minus")) {
                String invalidOp = main.getPluginConfig().getConfigData().getScoreInvalidOperationMessage();
                if (invalidOp != null) {
                    MessageUtils.sendMessage(sender, invalidOp);
                }
                return true;
            }

            int currentProgress = main.getEventManager().getPlayerProgress(targetUUID);
            int newProgress;

            if (operation.equals("plus")) {
                main.getEventManager().addPlayerProgress(targetUUID, amount);
                newProgress = currentProgress + amount;
                String scorePlus = main.getPluginConfig().getConfigData().getScorePlusMessage();
                if (scorePlus != null) {
                    String formattedMessage = scorePlus.replace("%amount%", String.valueOf(amount))
                            .replace("%player%", targetPlayerName)
                            .replace("%newscore%", String.valueOf(newProgress));
                    MessageUtils.sendMessage(sender, formattedMessage);
                }
            } else {
                newProgress = Math.max(0, currentProgress - amount);
                main.getEventManager().addPlayerProgress(targetUUID, newProgress - currentProgress);
                String scoreMinus = main.getPluginConfig().getConfigData().getScoreMinusMessage();
                if (scoreMinus != null) {
                    String formattedMessage = scoreMinus.replace("%amount%", String.valueOf(amount))
                            .replace("%player%", targetPlayerName)
                            .replace("%newscore%", String.valueOf(newProgress));
                    MessageUtils.sendMessage(sender, formattedMessage);
                }
            }

            if (newProgress >= main.getEventManager().getTargetAmount() && targetPlayer != null) {
                main.getEventManager().endEvent(targetPlayer);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start") && sender.hasPermission("fce.admin")) {
            if (main.getEventManager().isEventActive()) {
                String alreadyActive = main.getPluginConfig().getConfigData().getAlreadyActiveMessage();
                if (alreadyActive != null) {
                    MessageUtils.sendMessage(sender, alreadyActive);
                }
                return true;
            }

            main.getEventManager().stopTimers();

            if (args.length >= 4) {
                try {
                    Material targetItem = Material.matchMaterial(args[1].toUpperCase());
                    if (targetItem == null) {
                        String invalidItem = main.getPluginConfig().getConfigData().getInvalidItemMessage();
                        if (invalidItem != null) {
                            MessageUtils.sendMessage(sender, invalidItem);
                        }
                        return true;
                    }

                    int targetAmount;
                    if (args[2].contains("-")) {
                        targetAmount = RandomUtils.parseRandomRange(args[2]);
                    } else {
                        targetAmount = Integer.parseInt(args[2]);
                        if (targetAmount <= 0) {
                            String invalidAmount = main.getPluginConfig().getConfigData().getScoreInvalidAmountMessage();
                            if (invalidAmount != null) {
                                MessageUtils.sendMessage(sender, invalidAmount);
                            }
                            return true;
                        }
                    }

                    int durationMinutes;
                    if (args[3].contains("-")) {
                        durationMinutes = RandomUtils.parseRandomRange(args[3]);
                    } else {
                        durationMinutes = Integer.parseInt(args[3]);
                        if (durationMinutes <= 0) {
                            String invalidDuration = main.getPluginConfig().getConfigData().getScoreInvalidAmountMessage();
                            if (invalidDuration != null) {
                                MessageUtils.sendMessage(sender, invalidDuration);
                            }
                            return true;
                        }
                    }

                    main.getEventManager().setTargetItem(targetItem);
                    main.getEventManager().setTargetAmount(targetAmount);
                    main.getEventManager().setCustomEventDuration(durationMinutes * 1200L);

                } catch (NumberFormatException e) {
                    String invalidNumber = main.getPluginConfig().getConfigData().getScoreInvalidAmountMessage();
                    if (invalidNumber != null) {
                        MessageUtils.sendMessage(sender, invalidNumber);
                    }
                    return true;
                }
            }

            main.getEventManager().startEventTimer();
            String started = main.getPluginConfig().getConfigData().getEventStartedMessage();
            if (started != null) {
                MessageUtils.sendMessage(sender, started);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("fce.admin")) {
            if (!main.getEventManager().isEventActive()) {
                String noEvent = main.getPluginConfig().getConfigData().getNoEventMessage();
                if (noEvent != null) {
                    MessageUtils.sendMessage(sender, noEvent);
                }
                return true;
            }

            main.getEventManager().endEvent(null);
            String stopped = main.getPluginConfig().getConfigData().getEventStoppedMessage();
            if (stopped != null) {
                MessageUtils.sendMessage(sender, stopped);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("additem") && sender.hasPermission("fce.admin")) {

            if (args.length != 3) {
                String usage = main.getPluginConfig().getConfigData().getAddItemUsageMessage();
                if (usage != null) {
                    MessageUtils.sendMessage(sender, usage.replace("%label%", label));
                }
                return true;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                String noItem = main.getPluginConfig().getConfigData().getInvalidItemMessage();
                if (noItem != null) {
                    MessageUtils.sendMessage(sender, noItem);
                }
                return true;
            }
            Material itemType = itemInHand.getType();

            String range = args[1];
            if (!range.matches("\\d+-\\d+")) {
                String invalidRange = main.getPluginConfig().getConfigData().getAddItemInvalidRangeMessage();
                if (invalidRange != null) {
                    MessageUtils.sendMessage(sender, invalidRange);
                }
                return true;
            }
            String[] rangeParts = range.split("-");
            int min = Integer.parseInt(rangeParts[0]);
            int max = Integer.parseInt(rangeParts[1]);
            if (min <= 0 || max < min) {
                String invalidRangeValues = main.getPluginConfig().getConfigData().getAddItemInvalidRangeValuesMessage();
                if (invalidRangeValues != null) {
                    MessageUtils.sendMessage(sender, invalidRangeValues);
                }
                return true;
            }

            String translation = args[2];

            main.getPluginConfig().addEventItem(itemType, range, translation);

            String success = main.getPluginConfig().getConfigData().getAddItemSuccessMessage();
            if (success != null) {
                String formattedMessage = success
                        .replace("%item%", itemType.name())
                        .replace("%range%", range)
                        .replace("%translation%", translation);
                MessageUtils.sendMessage(sender, formattedMessage);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("fce.admin")) {
            main.getPluginConfig().createFiles();
            main.getEventManager().reloadEvent();
            String reloadMessage = main.getPluginConfig().getConfigData().getReloadMessage();
            if (reloadMessage != null) {
                MessageUtils.sendMessage(sender, reloadMessage);
            }
            return true;
        }

        String noPerm = main.getPluginConfig().getConfigData().getNoPermissionMessage();
        if (noPerm != null) {
            MessageUtils.sendMessage(sender, noPerm);
        }
        return true;
    }
}
