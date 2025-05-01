package me.trumbo.fastcollectevent.config.data;

import lombok.Getter;
import me.trumbo.fastcollectevent.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

@Getter
public class ConfigData {
    private MessageUtils.FormatType format;
    private String noPermissionMessage;
    private List<String> helpMessages;
    private String delayStartMessage;
    private String delayEndMessage;
    private String noEventMessage;
    private String noItemsMessage;
    private String collectedMessage;
    private String reloadMessage;
    private String alreadyActiveMessage;
    private String eventStartedMessage;
    private String eventStoppedMessage;
    private String scoreUsageMessage;
    private String scoreInvalidAmountMessage;
    private String scoreInvalidOperationMessage;
    private String scorePlusMessage;
    private String scoreMinusMessage;
    private String invalidItemMessage;
    private String addItemUsageMessage;
    private String addItemInvalidRangeMessage;
    private String addItemInvalidRangeValuesMessage;
    private String addItemSuccessMessage;

    private boolean databaseEnabled;
    private String databaseHost;
    private int databasePort;
    private String databaseName;
    private String databaseUsername;
    private String databasePassword;

    private int topLines;
    private String topHeader;
    private String topLine;
    private String topEmpty;

    private boolean bossbarEnabled;
    private String bossbarTitle;
    private String bossbarColor;
    private String bossbarStyle;

    public void load(FileConfiguration config) {
        String formatStr = config.getString("message-format");
        format = (formatStr != null && formatStr.equalsIgnoreCase("hex"))
                ? MessageUtils.FormatType.HEX
                : MessageUtils.FormatType.MINIMESSAGE;
        MessageUtils.setFormat(format);

        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            noPermissionMessage = messagesSection.getString("no-perm");
            helpMessages = messagesSection.getStringList("help");
            delayStartMessage = messagesSection.getString("delay-start");
            delayEndMessage = messagesSection.getString("delay-end");
            noEventMessage = messagesSection.getString("no-event");
            noItemsMessage = messagesSection.getString("no-items");
            collectedMessage = messagesSection.getString("collected");
            reloadMessage = messagesSection.getString("reload");
            alreadyActiveMessage = messagesSection.getString("already-active");
            eventStartedMessage = messagesSection.getString("event-started");
            eventStoppedMessage = messagesSection.getString("event-stopped");
            scoreUsageMessage = messagesSection.getString("score-usage");
            scoreInvalidAmountMessage = messagesSection.getString("score-invalid-amount");
            scoreInvalidOperationMessage = messagesSection.getString("score-invalid-operation");
            scorePlusMessage = messagesSection.getString("score-plus");
            scoreMinusMessage = messagesSection.getString("score-minus");
            invalidItemMessage = messagesSection.getString("invalid-item");
            addItemUsageMessage = messagesSection.getString("additem-usage");
            addItemInvalidRangeMessage = messagesSection.getString("additem-invalid-range");
            addItemInvalidRangeValuesMessage = messagesSection.getString("additem-invalid-range-values");
            addItemSuccessMessage = messagesSection.getString("additem-success");
        }

        ConfigurationSection databaseSection = config.getConfigurationSection("database");
        if (databaseSection != null) {
            databaseEnabled = databaseSection.getBoolean("enabled");
            databaseHost = databaseSection.getString("host");
            databasePort = databaseSection.getInt("port");
            databaseName = databaseSection.getString("database");
            databaseUsername = databaseSection.getString("username");
            databasePassword = databaseSection.getString("password");
        }

        ConfigurationSection topSection = config.getConfigurationSection("top-settings");
        if (topSection != null) {
            topLines = topSection.getInt("lines");
            topHeader = topSection.getString("top-header");
            topLine = topSection.getString("top-line");
            topEmpty = topSection.getString("top-empty");
        }

        ConfigurationSection bossbarSection = config.getConfigurationSection("bossbar");
        if (bossbarSection != null) {
            bossbarEnabled = bossbarSection.getBoolean("enabled");
            bossbarTitle = bossbarSection.getString("title");
            bossbarColor = bossbarSection.getString("color");
            bossbarStyle = bossbarSection.getString("style");
        }
    }
}