package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateCommand implements CommandExecutor {
    
    private final MightyRTP plugin;
    
    public UpdateCommand(MightyRTP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has permission
        if (!sender.hasPermission("mightyrtp.update")) {
            if (sender instanceof Player) {
                sender.sendMessage("§c[MightyRTP] You don't have permission to use this command!");
            } else {
                sender.sendMessage("You don't have permission to use this command!");
            }
            return true;
        }
        
        // Check for updates
        if (sender instanceof Player) {
            plugin.checkForUpdatesManually((Player) sender);
        } else {
            plugin.checkForUpdatesManually();
            sender.sendMessage("Checking for updates...");
        }
        
        return true;
    }
}

