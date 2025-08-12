package com.example.mightyrtp.managers;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final MightyRTP plugin;
    private final Map<UUID, PlayerCooldown> playerCooldowns;
    
    public CooldownManager(MightyRTP plugin) {
        this.plugin = plugin;
        this.playerCooldowns = new HashMap<>();
    }
    
    public boolean canUseCommand(Player player) {
        // Check if player has bypass permission
        if (player.hasPermission("mightyrtp.bypass")) {
            return true;
        }
        
        // Check if cooldown is enabled
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        PlayerCooldown cooldown = playerCooldowns.get(playerId);
        
        long currentTime = System.currentTimeMillis();
        
        if (cooldown == null) {
            // First time using the command - start a new cooldown
            cooldown = new PlayerCooldown(currentTime);
            playerCooldowns.put(playerId, cooldown);
            return true;
        }
        
        // Check if the current cooldown window has expired
        long cooldownEndTime = cooldown.getStartTime() + (plugin.getConfigManager().getCooldownTimeWindow() * 60 * 1000);
        
        if (currentTime >= cooldownEndTime) {
            // Cooldown window expired, start a new one
            cooldown = new PlayerCooldown(currentTime);
            playerCooldowns.put(playerId, cooldown);
            return true;
        }
        
        // Check if player can use the command within the current window
        if (cooldown.getUseCount() >= plugin.getConfigManager().getCooldownMaxUses()) {
            return false;
        }
        
        // Add current use to existing cooldown window
        cooldown.addUse(currentTime);
        return true;
    }
    
    public int getRemainingUses(Player player) {
        // If player has bypass permission, return unlimited uses
        if (player.hasPermission("mightyrtp.bypass")) {
            return Integer.MAX_VALUE;
        }
        
        // If cooldown is disabled, return max uses
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return plugin.getConfigManager().getCooldownMaxUses();
        }
        
        UUID playerId = player.getUniqueId();
        PlayerCooldown cooldown = playerCooldowns.get(playerId);
        
        if (cooldown == null) {
            return plugin.getConfigManager().getCooldownMaxUses();
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownEndTime = cooldown.getStartTime() + (plugin.getConfigManager().getCooldownTimeWindow() * 60 * 1000);
        
        // If cooldown window has expired, reset remaining uses
        if (currentTime >= cooldownEndTime) {
            return plugin.getConfigManager().getCooldownMaxUses();
        }
        
        return plugin.getConfigManager().getCooldownMaxUses() - cooldown.getUseCount();
    }
    
    public long getTimeUntilReset(Player player) {
        // If player has bypass permission, no time to wait
        if (player.hasPermission("mightyrtp.bypass")) {
            return 0;
        }
        
        // If cooldown is disabled, return 0 (no time to wait)
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        PlayerCooldown cooldown = playerCooldowns.get(playerId);
        
        if (cooldown == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long cooldownEndTime = cooldown.getStartTime() + (plugin.getConfigManager().getCooldownTimeWindow() * 60 * 1000);
        
        if (currentTime >= cooldownEndTime) {
            return 0; // Cooldown has already reset
        }
        
        return cooldownEndTime - currentTime;
    }
    
    public void clearCooldown(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }
    
    private static class PlayerCooldown {
        private final long startTime;
        private final java.util.List<Long> useTimes;
        
        public PlayerCooldown(long startTime) {
            this.startTime = startTime;
            this.useTimes = new java.util.ArrayList<>();
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void addUse(long time) {
            useTimes.add(time);
        }
        
        public int getUseCount() {
            return useTimes.size();
        }
    }
}
