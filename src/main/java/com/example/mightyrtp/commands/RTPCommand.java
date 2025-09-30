package com.example.mightyrtp.commands;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.MessageManager;
import com.example.mightyrtp.managers.CooldownManager;
import com.example.mightyrtp.utils.TeleportUtils;
import com.example.mightyrtp.utils.TeleportResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
        final Player targetPlayer;
        boolean isConsole = !(sender instanceof Player);
        
        // Handle console execution for Multiverse-CommandDestination compatibility
        if (isConsole) {
            // Support both formats:
            // 1. BetterRTP format: rtp player %player% world
            // 2. Direct format: rtp %player% [world]
            if (args.length >= 2 && args[0].equalsIgnoreCase("player")) {
                // BetterRTP format: rtp player %player% world
                String playerName = args[1];
                String worldName = args.length > 2 ? args[2] : null;
                
                targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage("Player " + playerName + " not found!");
                    return true;
                }
                
                // Handle world-specific RTP for console
                if (worldName != null && !worldName.equalsIgnoreCase("world")) {
                    World targetWorld = Bukkit.getWorld(worldName);
                    if (targetWorld != null) {
                        // Console can always do world-specific RTP
                        executeRTPInWorld(targetPlayer, targetWorld, sender, true, true); // true = isConsole, true = fastMode
                        return true;
                    } else {
                        sender.sendMessage("World " + worldName + " not found!");
                        return true;
                    }
                }
                
                // Console commands bypass all cooldowns and permissions
                executeRTP(targetPlayer, sender, true, true); // true = isConsole, true = fastMode
                return true;
            } else if (args.length >= 1) {
                // Direct format: rtp %player% [world]
                String playerName = args[0];
                String worldName = args.length > 1 ? args[1] : null;
                
                targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage("Player " + playerName + " not found!");
                    return true;
                }
                
                // Handle world-specific RTP for console
                if (worldName != null) {
                    World targetWorld = Bukkit.getWorld(worldName);
                    if (targetWorld != null) {
                        // Console can always do world-specific RTP
                        executeRTPInWorld(targetPlayer, targetWorld, sender, true, true); // true = isConsole, true = fastMode
                        return true;
                    } else {
                        sender.sendMessage("World " + worldName + " not found!");
                        return true;
                    }
                }
                
                // Console commands bypass all cooldowns and permissions
                executeRTP(targetPlayer, sender, true, true); // true = isConsole, true = fastMode
                return true;
            } else {
                sender.sendMessage("Console usage:");
                sender.sendMessage("  /rtp player <player> [world]  - BetterRTP format");
                sender.sendMessage("  /rtp <player> [world]         - Direct format");
                return true;
            }
        }
        
        // Player execution (existing logic)
        Player player = (Player) sender;
        
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
        
        // Check for world-specific RTP
        if (args.length > 1) {
            if (!player.hasPermission("mightyrtp.rtp.world")) {
                messageManager.sendMessage(player, "no-permission");
                return true;
            }
            
            String worldName = args[1];
            World targetWorld = Bukkit.getWorld(worldName);
            if (targetWorld != null) {
                // Check cooldown for the target player (only for player commands)
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
                    
                    String messageKey = "cooldown-exceeded";
                    String message = messageManager.getMessage(messageKey).replace("{time}", timeString);
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
                
                executeRTPInWorld(targetPlayer, targetWorld, player, false, false);
                return true;
            } else {
                messageManager.sendMessage(player, "world-not-found");
                return true;
            }
        }
        
        // Check cooldown for the target player (only for player commands)
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
        
        executeRTP(targetPlayer, player, false, false);
        return true;
    }
    
    private void executeRTP(Player targetPlayer, CommandSender sender, boolean isConsole, boolean fastMode) {
        // Check if world is blacklisted
        if (configManager.isWorldBlacklisted(targetPlayer.getWorld().getName())) {
            if (isConsole) {
                sender.sendMessage("World " + targetPlayer.getWorld().getName() + " is blacklisted for RTP!");
            } else {
                messageManager.sendMessage((Player) sender, "world-blacklisted");
            }
            return;
        }
        
        // Show title message to target player (based on config for console commands)
        if (!isConsole || configManager.shouldShowTitlesForConsole()) {
            messageManager.showTitle(targetPlayer);
        }
        
        // Send teleporting message
        if (isConsole) {
            sender.sendMessage("Teleporting " + targetPlayer.getName() + " to random location...");
            messageManager.sendMessage(targetPlayer, "teleporting");
        } else {
            Player player = (Player) sender;
            if (targetPlayer.equals(player)) {
                messageManager.sendMessage(player, "teleporting");
            } else {
                String message = messageManager.getMessage("teleporting-other").replace("{player}", targetPlayer.getName());
                String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                player.sendMessage(formattedMessage);
                messageManager.sendMessage(targetPlayer, "teleporting");
            }
        }
        
        // Find safe location and teleport (async)
        teleportUtils.findSafeLocationAsync(targetPlayer.getWorld(), targetPlayer.getLocation(), targetPlayer, result -> {
            if (result.isSuccess()) {
                targetPlayer.teleport(result.getLocation());
                if (isConsole) {
                    sender.sendMessage("Successfully teleported " + targetPlayer.getName() + " to random location!");
                    messageManager.sendMessage(targetPlayer, "teleported");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "teleported");
                    } else {
                        String message = messageManager.getMessage("teleported-other").replace("{player}", targetPlayer.getName());
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "teleported");
                    }
                }
            } else if (result.isTimeout()) {
                if (isConsole) {
                    sender.sendMessage("Failed to teleport " + targetPlayer.getName() + ": Search timeout!");
                    messageManager.sendMessage(targetPlayer, "search-timeout");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "search-timeout");
                    } else {
                        String message = messageManager.getMessage("search-timeout");
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "search-timeout");
                    }
                }
            } else {
                if (isConsole) {
                    sender.sendMessage("Failed to teleport " + targetPlayer.getName() + ": No safe location found!");
                    messageManager.sendMessage(targetPlayer, "no-safe-location");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "no-safe-location");
                    } else {
                        String message = messageManager.getMessage("no-safe-location-other").replace("{player}", targetPlayer.getName());
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "no-safe-location");
                    }
                }
            }
        });
    }

    private void executeRTPInWorld(Player targetPlayer, World targetWorld, CommandSender sender, boolean isConsole, boolean fastMode) {
        // Check if world is blacklisted
        if (configManager.isWorldBlacklisted(targetWorld.getName())) {
            if (isConsole) {
                sender.sendMessage("World " + targetWorld.getName() + " is blacklisted for RTP!");
            } else {
                messageManager.sendMessage((Player) sender, "world-blacklisted");
            }
            return;
        }

        // Show title message to target player (based on config for console commands)
        if (!isConsole || configManager.shouldShowTitlesForConsole()) {
            messageManager.showTitle(targetPlayer);
        }

        // Send teleporting message
        if (isConsole) {
            sender.sendMessage("Teleporting " + targetPlayer.getName() + " to random location in " + targetWorld.getName() + "...");
            messageManager.sendMessage(targetPlayer, "teleporting");
        } else {
            Player player = (Player) sender;
            if (targetPlayer.equals(player)) {
                messageManager.sendMessage(player, "teleporting");
            } else {
                String message = messageManager.getMessage("teleporting-other").replace("{player}", targetPlayer.getName());
                String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                player.sendMessage(formattedMessage);
                messageManager.sendMessage(targetPlayer, "teleporting");
            }
        }

        // Find safe location and teleport (async)
        teleportUtils.findSafeLocationAsync(targetWorld, targetPlayer.getLocation(), targetPlayer, result -> {
            if (result.isSuccess()) {
                targetPlayer.teleport(result.getLocation());
                if (isConsole) {
                    sender.sendMessage("Successfully teleported " + targetPlayer.getName() + " to random location in " + targetWorld.getName() + "!");
                    messageManager.sendMessage(targetPlayer, "teleported");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "teleported");
                    } else {
                        String message = messageManager.getMessage("teleported-other").replace("{player}", targetPlayer.getName());
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "teleported");
                    }
                }
            } else if (result.isTimeout()) {
                if (isConsole) {
                    sender.sendMessage("Failed to teleport " + targetPlayer.getName() + ": Search timeout!");
                    messageManager.sendMessage(targetPlayer, "search-timeout");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "search-timeout");
                    } else {
                        String message = messageManager.getMessage("search-timeout");
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "search-timeout");
                    }
                }
            } else {
                if (isConsole) {
                    sender.sendMessage("Failed to teleport " + targetPlayer.getName() + ": No safe location found!");
                    messageManager.sendMessage(targetPlayer, "no-safe-location");
                } else {
                    Player player = (Player) sender;
                    if (targetPlayer.equals(player)) {
                        messageManager.sendMessage(player, "no-safe-location");
                    } else {
                        String message = messageManager.getMessage("no-safe-location-other").replace("{player}", targetPlayer.getName());
                        String formattedMessage = messageManager.parseColorTagsToLegacy(message);
                        player.sendMessage(formattedMessage);
                        messageManager.sendMessage(targetPlayer, "no-safe-location");
                    }
                }
            }
        });
    }
}
