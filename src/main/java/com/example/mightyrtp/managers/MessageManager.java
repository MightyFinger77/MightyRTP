package com.example.mightyrtp.managers;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {
    
    private final MightyRTP plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    public MessageManager(MightyRTP plugin) {
        this.plugin = plugin;
    }
    
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public boolean areTitlesEnabled() {
        return plugin.getConfigManager().areTitlesEnabled();
    }
    
    public void showTitle(Player player) {
        if (!areTitlesEnabled()) {
            return;
        }
        
        String titleText = messagesConfig.getString("title.text", "<green>Teleporting to the wild...</green>");
        String subtitleText = messagesConfig.getString("subtitle.text", "<aqua>Good luck out there!</aqua>");
        
        // Convert color tags to legacy color codes
        String formattedTitle = parseColorTagsToLegacy(titleText);
        String formattedSubtitle = parseColorTagsToLegacy(subtitleText);
        
        // Use legacy title API for compatibility
        player.sendTitle(formattedTitle, formattedSubtitle, 10, 40, 10);
    }
    
    public String parseColorTagsToLegacy(String text) {
        // Convert <color> tags to legacy color codes
        return text
            .replace("<black>", "§0")
            .replace("</black>", "")
            .replace("<dark_blue>", "§1")
            .replace("</dark_blue>", "")
            .replace("<dark_green>", "§2")
            .replace("</dark_green>", "")
            .replace("<dark_aqua>", "§3")
            .replace("</dark_aqua>", "")
            .replace("<dark_red>", "§4")
            .replace("</dark_red>", "")
            .replace("<dark_purple>", "§5")
            .replace("</dark_purple>", "")
            .replace("<gold>", "§6")
            .replace("</gold>", "")
            .replace("<gray>", "§7")
            .replace("</gray>", "")
            .replace("<dark_gray>", "§8")
            .replace("</dark_gray>", "")
            .replace("<blue>", "§9")
            .replace("</blue>", "")
            .replace("<green>", "§a")
            .replace("</green>", "")
            .replace("<aqua>", "§b")
            .replace("</aqua>", "")
            .replace("<red>", "§c")
            .replace("</red>", "")
            .replace("<light_purple>", "§d")
            .replace("</light_purple>", "")
            .replace("<yellow>", "§e")
            .replace("</yellow>", "")
            .replace("<white>", "§f")
            .replace("</white>", "")
            .replace("<reset>", "§r")
            .replace("</reset>", "")
            .replace("<bold>", "§l")
            .replace("</bold>", "")
            .replace("<italic>", "§o")
            .replace("</italic>", "")
            .replace("<underlined>", "§n")
            .replace("</underlined>", "")
            .replace("<strikethrough>", "§m")
            .replace("</strikethrough>", "");
    }
    
    public String getMessage(String key) {
        return messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
    }
    
    public void sendMessage(Player player, String key) {
        String message = getMessage(key);
        // Convert color tags to legacy color codes
        String formattedMessage = parseColorTagsToLegacy(message);
        player.sendMessage(formattedMessage);
    }
}
