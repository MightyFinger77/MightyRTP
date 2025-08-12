package com.example.mightyrtp.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RTPTabCompleter implements TabCompleter {
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // If sender has permission to teleport others, show online players
            if (sender.hasPermission("mightyrtp.rtp.other")) {
                List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                
                // Filter based on what the user has typed
                String input = args[0].toLowerCase();
                for (String playerName : onlinePlayers) {
                    if (playerName.toLowerCase().startsWith(input)) {
                        completions.add(playerName);
                    }
                }
            }
        }
        
        return completions;
    }
}
