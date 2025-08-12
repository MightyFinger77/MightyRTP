package com.example.mightyrtp.managers;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    
    private final MightyRTP plugin;
    private FileConfiguration config;
    
    public ConfigManager(MightyRTP plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public List<String> getBlacklistedWorlds() {
        return config.getStringList("blacklisted-worlds");
    }
    
    public int getTeleportDistance() {
        return config.getInt("teleport-distance", 5000);
    }
    
    public int getMinDistanceFromSpawn() {
        return config.getInt("min-distance-from-spawn", 500);
    }
    
    public List<Material> getSafeBlocks() {
        List<String> safeBlockNames = config.getStringList("safe-blocks");
        return safeBlockNames.stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material name in config: " + name);
                        return null;
                    }
                })
                .filter(material -> material != null)
                .collect(Collectors.toList());
    }
    
    public List<Material> getUnsafeBlocks() {
        List<String> unsafeBlockNames = config.getStringList("unsafe-blocks");
        return unsafeBlockNames.stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material name in config: " + name);
                        return null;
                    }
                })
                .filter(material -> material != null)
                .collect(Collectors.toList());
    }
    
    public boolean isWorldBlacklisted(String worldName) {
        return getBlacklistedWorlds().contains(worldName);
    }
    
    public int getCooldownMaxUses() {
        return config.getInt("cooldown.max-uses", 10);
    }
    
    public int getCooldownTimeWindow() {
        return config.getInt("cooldown.time-window", 10);
    }
    
    public boolean isCooldownEnabled() {
        return config.getBoolean("cooldown.enabled", true);
    }
    
    public boolean areTitlesEnabled() {
        return config.getBoolean("titles.enabled", true);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    public int getDebugLogAttemptInterval() {
        return config.getInt("debug.log-attempt-interval", 10);
    }
    
    public boolean isAsyncTeleportSearchEnabled() {
        return config.getBoolean("performance.async-teleport-search", true);
    }
    
    public int getMaxSearchTimePerAttempt() {
        return config.getInt("performance.max-search-time-per-attempt", 50);
    }
    
    /**
     * Get the safety strictness level
     * @return The safety strictness level (1-5)
     */
    public int getSafetyStrictness() {
        return config.getInt("safety.strictness", 3);
    }

    /**
     * Get the maximum number of attempts to find a safe location
     * @return The maximum number of attempts
     */
    public int getMaxAttempts() {
        return config.getInt("safety.max-attempts", 50);
    }
    

}
