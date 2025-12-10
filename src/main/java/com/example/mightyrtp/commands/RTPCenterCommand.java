package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.CentersManager;
import com.example.mightyrtp.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPCenterCommand implements CommandExecutor {
    
    private final MightyRTP plugin;
    private final CentersManager centersManager;
    private final MessageManager messageManager;
    
    public RTPCenterCommand(MightyRTP plugin) {
        this.plugin = plugin;
        this.centersManager = plugin.getCentersManager();
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
        
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        // Set the center for this world
        centersManager.setCenter(world, x, z);
        
        // Send success message
        player.sendMessage("§a[MightyRTP] RTP center set to §e" + x + ", " + z + " §ain world §e" + world.getName());
        player.sendMessage("§7[MightyRTP] All RTP teleports in this world will now use this location as the center point.");
        
        plugin.getLogger().info("RTP center set to " + x + ", " + z + " in world " + world.getName() + " by " + player.getName());
        
        return true;
    }
}

