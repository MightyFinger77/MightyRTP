package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.MessageManager;
import com.example.mightyrtp.managers.CooldownManager;
import com.example.mightyrtp.utils.TeleportUtils;
import com.example.mightyrtp.utils.TeleportResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPCommand implements CommandExecutor {
    
    private final MightyRTP plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final CooldownManager cooldownManager;
    private final TeleportUtils teleportUtils;
    
    public RTPCommand(MightyRTP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.cooldownManager = plugin.getCooldownManager();
        this.teleportUtils = new TeleportUtils(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        final Player targetPlayer; // Default to self
        
        // Check if player has permission
        if (!player.hasPermission("mightyrtp.rtp")) {
            messageManager.sendMessage(player, "no-permission");
            return true;
        }
        
        // Check if player wants to teleport someone else
        if (args.length > 0) {
            if (!player.hasPermission("mightyrtp.rtp.other")) {
                messageManager.sendMessage(player, "no-permission");
                return true;
            }
            
            Player foundPlayer = Bukkit.getPlayer(args[0]);
            if (foundPlayer == null) {
                String message = messageManager.getMessage("player-not-found").replace("{player}", args[0]);
                String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                player.sendMessage(formattedMessage);
                return true;
            }
            targetPlayer = foundPlayer;
        } else {
            targetPlayer = player;
        }
        
        // Check cooldown for the target player
        if (!cooldownManager.canUseCommand(targetPlayer)) {
            long timeUntilReset = cooldownManager.getTimeUntilReset(targetPlayer);
            int minutes = (int) (timeUntilReset / (60 * 1000));
            int seconds = (int) ((timeUntilReset % (60 * 1000)) / 1000);
            
            String timeString;
            if (minutes > 0) {
                timeString = minutes + " minutes " + seconds + " seconds";
            } else {
                timeString = seconds + " seconds";
            }
            
            // Use MessageManager to send the message with proper color parsing
            String messageKey = "cooldown-exceeded";
            String message = messageManager.getMessage(messageKey).replace("{time}", timeString);
            // Parse the color tags manually since we need to replace placeholders
            String formattedMessage = messageManager.parseColorTagsToLegacy(message);
            player.sendMessage(formattedMessage);
            return true;
        }
        
        // Show bypass status if player has bypass permission
        if (targetPlayer.hasPermission("mightyrtp.bypass")) {
            String message = messageManager.getMessage("bypass-active");
            String formattedMessage = messageManager.parseColorTagsToLegacy(message);
            player.sendMessage(formattedMessage);
        }
        
        // Check if world is blacklisted
        if (configManager.isWorldBlacklisted(targetPlayer.getWorld().getName())) {
            messageManager.sendMessage(player, "world-blacklisted");
            return true;
        }
        
        // Show title message to target player
        messageManager.showTitle(targetPlayer);
        
        // Send teleporting message
        if (targetPlayer.equals(player)) {
            messageManager.sendMessage(player, "teleporting");
        } else {
            String message = messageManager.getMessage("teleporting-other").replace("{player}", targetPlayer.getName());
            String formattedMessage = messageManager.parseColorTagsToLegacy(message);
            player.sendMessage(formattedMessage);
            messageManager.sendMessage(targetPlayer, "teleporting");
        }
        
        // Find safe location and teleport (async)
        teleportUtils.findSafeLocationAsync(targetPlayer.getWorld(), targetPlayer.getLocation(), targetPlayer, result -> {
            if (result.isSuccess()) {
                targetPlayer.teleport(result.getLocation());
                if (targetPlayer.equals(player)) {
                    messageManager.sendMessage(player, "teleported");
                } else {
                    String message = messageManager.getMessage("teleported-other").replace("{player}", targetPlayer.getName());
                    String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                    player.sendMessage(formattedMessage);
                    messageManager.sendMessage(targetPlayer, "teleported");
                }
            } else if (result.isTimeout()) {
                if (targetPlayer.equals(player)) {
                    messageManager.sendMessage(player, "search-timeout");
                } else {
                    String message = messageManager.getMessage("search-timeout");
                    String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                    player.sendMessage(formattedMessage);
                    messageManager.sendMessage(targetPlayer, "search-timeout");
                }
            } else {
                if (targetPlayer.equals(player)) {
                    messageManager.sendMessage(player, "no-safe-location");
                } else {
                    String message = messageManager.getMessage("no-safe-location-other").replace("{player}", targetPlayer.getName());
                    String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                    player.sendMessage(formattedMessage);
                    messageManager.sendMessage(targetPlayer, "no-safe-location");
                }
            }
        });
        
        return true;
    }
}
