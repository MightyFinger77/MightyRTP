package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.SpotsManager;
import com.example.mightyrtp.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPSetCommand implements CommandExecutor {
    
    private final MightyRTP plugin;
    private final SpotsManager spotsManager;
    private final MessageManager messageManager;
    
    public RTPSetCommand(MightyRTP plugin) {
        this.plugin = plugin;
        this.spotsManager = plugin.getSpotsManager();
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has permission
        if (!sender.hasPermission("mightyrtp.admin")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "no-permission");
            } else {
                sender.sendMessage("You don't have permission to use this command!");
            }
            return true;
        }
        
        // Command can only be executed by players (need their location)
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Get player's current location
        org.bukkit.Location location = player.getLocation();
        org.bukkit.World world = location.getWorld();
        
        if (world == null) {
            player.sendMessage("§c[MightyRTP] Could not determine your current world!");
            return true;
        }
        
        // Add the spot
        spotsManager.addSpot(world, location);
        
        int spotCount = spotsManager.getSpotCount(world);
        
        // Send success message
        player.sendMessage("§a[MightyRTP] Teleport spot added at §e" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " §ain world §e" + world.getName());
        player.sendMessage("§7[MightyRTP] Total spots in this world: §e" + spotCount);
        player.sendMessage("§7[MightyRTP] Remember to set teleport-distance to 'CUSTOM' in config.yml for these spots to be used.");
        
        plugin.getLogger().info("Teleport spot added at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in world " + world.getName() + " by " + player.getName());
        
        return true;
    }
}

