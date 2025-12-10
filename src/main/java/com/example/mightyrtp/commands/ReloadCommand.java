package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.MessageManager;
import com.example.mightyrtp.managers.CentersManager;
import com.example.mightyrtp.managers.SpotsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {
    
    private final MightyRTP plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    
    public ReloadCommand(MightyRTP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has permission
        if (!sender.hasPermission("mightyrtp.reload")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "no-permission");
            } else {
                sender.sendMessage("You don't have permission to use this command!");
            }
            return true;
        }
        
        try {
            // Reload all configuration files
            configManager.reloadConfig();
            messageManager.reloadMessages();
            plugin.getCentersManager().reloadCenters();
            plugin.getSpotsManager().reloadSpots();
            
            // Send success message
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "reload-success");
            } else {
                sender.sendMessage("Configuration reloaded successfully!");
            }
            
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
            
        } catch (Exception e) {
            // Send failure message
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "reload-failed");
            } else {
                sender.sendMessage("Failed to reload configuration!");
            }
            
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}
