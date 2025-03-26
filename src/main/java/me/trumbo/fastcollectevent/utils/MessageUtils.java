package me.trumbo.fastcollectevent.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final char COLOR_CHAR = ChatColor.COLOR_CHAR;
    private static FormatType currentFormat = FormatType.HEX;

    public enum FormatType {
        HEX(MessageUtils::formatHex),
        MINIMESSAGE(MessageUtils::formatMiniMessage);

        private final Function<String, Object> formatter;

        FormatType(Function<String, Object> formatter) {
            this.formatter = formatter;
        }

        public Object format(String message) {
            return formatter.apply(message);
        }
    }

    public static void setFormat(FormatType format) {
        if (format != null) currentFormat = format;
    }

    public static Object format(String message) {
        if (message == null) return null;
        return currentFormat.format(message);
    }

    public static void sendMessageToAll(String message) {
        if (message == null) return;
        Object formatted = format(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessageToPlayer(player, formatted);
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) return;

        if (sender instanceof Player) {
            sendMessageToPlayer((Player) sender, message);
        } else {
            sender.sendMessage(formatLegacy(message));
        }
    }

    public static void sendMessageToPlayer(Player player, String message) {
        if (player == null || message == null) return;
        sendMessageToPlayer(player, format(message));
    }

    private static void sendMessageToPlayer(Player player, Object formattedMessage) {
        if (formattedMessage instanceof Component) {
            player.sendMessage((Component) formattedMessage);
        } else if (formattedMessage instanceof String) {
            player.sendMessage((String) formattedMessage);
        }
    }

    private static String formatLegacy(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String formatHex(String message) {
        try {
            String legacyFormatted = formatLegacy(message);
            Matcher matcher = HEX_PATTERN.matcher(legacyFormatted);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String hex = matcher.group(1);
                String replacement = COLOR_CHAR + "x"
                        + COLOR_CHAR + hex.charAt(0) + COLOR_CHAR + hex.charAt(1)
                        + COLOR_CHAR + hex.charAt(2) + COLOR_CHAR + hex.charAt(3)
                        + COLOR_CHAR + hex.charAt(4) + COLOR_CHAR + hex.charAt(5);
                matcher.appendReplacement(result, replacement);
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            return formatLegacy(message);
        }
    }

    private static Component formatMiniMessage(String message) {
        try {
            return MiniMessage.miniMessage().deserialize(message);
        } catch (Exception e) {
            return Component.text(formatLegacy(message));
        }
    }
}
