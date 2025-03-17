package me.trumbo.fastcollectevent.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final char COLOR_CHAR = ChatColor.COLOR_CHAR;

    public enum FormatType {
        HEX,
        MINIMESSAGE
    }

    private MessageUtils() {}

    public static Object format(String message, FormatType format) {
        if (message == null) return null;

        switch (format) {
            case HEX:
                return formatHex(message);
            case MINIMESSAGE:
                return formatMiniMessage(message);
            default:
                return message;
        }
    }

    public static void sendMessageToAll(String message, FormatType format) {
        if (message == null) return;

        Object formatted = format(message, format);
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessageToPlayer(player, formatted);
        }
    }

    public static void sendMessageToPlayer(Player player, String message, FormatType format) {
        if (player == null || message == null) return;
        sendMessageToPlayer(player, format(message, format));
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
    }

    private static Component formatMiniMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }
}
