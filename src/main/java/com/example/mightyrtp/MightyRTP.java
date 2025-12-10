package com.example.mightyrtp;

import com.example.mightyrtp.commands.RTPCommand;
import com.example.mightyrtp.commands.RTPTabCompleter;
import com.example.mightyrtp.commands.ReloadCommand;
import com.example.mightyrtp.commands.UpdateCommand;
import com.example.mightyrtp.commands.RTPCenterCommand;
import com.example.mightyrtp.commands.RTPSetCommand;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.MessageManager;
import com.example.mightyrtp.managers.CooldownManager;
import com.example.mightyrtp.managers.CentersManager;
import com.example.mightyrtp.managers.SpotsManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class MightyRTP extends JavaPlugin implements Listener {
    
    private static MightyRTP instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private CentersManager centersManager;
    private SpotsManager spotsManager;
    
    // Spigot resource ID for update checking
    // TODO: Replace with actual Spigot resource ID when available
    private static final int SPIGOT_RESOURCE_ID = 0; // Placeholder - update with actual resource ID
    
    // Store update info for new players
    private String latestVersion = null;
    private boolean updateAvailable = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        cooldownManager = new CooldownManager(this);
        centersManager = new CentersManager(this);
        spotsManager = new SpotsManager(this);
        
        // Load configurations (with migration)
        configManager.loadConfig();
        
        // Load centers
        centersManager.loadCenters();
        
        // Load spots
        spotsManager.loadSpots();
        
        // Migrate other config files
        migrateConfigFile("messages.yml");
        
        // Load messages after migration
        messageManager.loadMessages();
        
        // Register commands
        getCommand("rtp").setExecutor(new RTPCommand(this));
        getCommand("rtp").setTabCompleter(new RTPTabCompleter());
        getCommand("rtp-reload").setExecutor(new ReloadCommand(this));
        getCommand("rtp-update").setExecutor(new UpdateCommand(this));
        getCommand("rtp-center").setExecutor(new RTPCenterCommand(this));
        getCommand("rtp-set").setExecutor(new RTPSetCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        
        // Check for updates if enabled
        if (configManager.isUpdateCheckEnabled()) {
            checkForUpdates();
        }
        
        getLogger().info("MightyRTP 1.0.5 has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MightyRTP 1.0.5 has been disabled!");
    }
    
    public static MightyRTP getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public CentersManager getCentersManager() {
        return centersManager;
    }
    
    public SpotsManager getSpotsManager() {
        return spotsManager;
    }
    
    /**
     * Check if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        if (configManager == null || configManager.getConfig() == null) {
            return false;
        }
        return configManager.getConfig().getBoolean("debug.enabled", false);
    }
    
    /**
     * Get the config FileConfiguration (for migration)
     */
    private FileConfiguration getConfigInternal() {
        if (configManager == null) {
            return null;
        }
        return configManager.getConfig();
    }
    
    /**
     * Check if update checking is enabled
     */
    public boolean isUpdateCheckEnabled() {
        if (configManager == null) {
            return true; // Default to enabled if config not loaded
        }
        return configManager.isUpdateCheckEnabled();
    }
    
    /**
     * Public method to manually check for updates (can be called from commands)
     */
    public void checkForUpdatesManually() {
        checkForUpdatesManually(null);
    }
    
    /**
     * Public method to manually check for updates with player feedback
     */
    public void checkForUpdatesManually(Player player) {
        if (isUpdateCheckEnabled()) {
            getLogger().info("Manually checking for updates...");
            checkForUpdates(player);
        } else {
            getLogger().info("Update checking is disabled in config");
            if (player != null) {
                player.sendMessage("§c[MightyRTP] Update checking is disabled in config");
            }
        }
    }
    
    /**
     * Check for plugin updates using SpigotMC API
     */
    private void checkForUpdates() {
        checkForUpdates(null);
    }
    
    /**
     * Check for plugin updates using SpigotMC API with player feedback
     */
    private void checkForUpdates(Player player) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String url = "https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_RESOURCE_ID;
                java.net.URLConnection connection = java.net.URI.create(url).toURL().openConnection();
                connection.setRequestProperty("User-Agent", "MightyRTP-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                String latestVersion;
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()))) {
                    latestVersion = reader.readLine();
                }
                
                String currentVersion = getDescription().getVersion();
                
                if (isDebugEnabled()) {
                    getLogger().info("[DEBUG] Update check - API returned: '" + latestVersion + "', Current: '" + currentVersion + "'");
                }
                
                if (isNewerVersion(latestVersion, currentVersion)) {
                    // Store update info for new players
                    this.latestVersion = latestVersion;
                    this.updateAvailable = true;
                    
                    getServer().getScheduler().runTask(this, () -> {
                        getLogger().info("§a[MightyRTP] Update available: " + latestVersion);
                        getLogger().info("§a[MightyRTP] Current version: " + currentVersion);
                        getLogger().info("§a[MightyRTP] Download: https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID);
                        
                        // Send update message to the player who requested the check
                        if (player != null) {
                            player.sendMessage("§a[MightyRTP] §eUpdate available: §f" + latestVersion + " §7(current: " + currentVersion + ")");
                            player.sendMessage("§a[MightyRTP] §7Download: §9https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID);
                        }
                        
                        // Send update message to all online OP'd players with a delay to show after MOTD
                        getServer().getScheduler().runTaskLater(this, () -> {
                            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                                if (onlinePlayer.isOp() && (player == null || !onlinePlayer.equals(player))) {
                                    onlinePlayer.sendMessage("§a[MightyRTP] §eUpdate available: §f" + latestVersion + " §7(current: " + currentVersion + ")");
                                    onlinePlayer.sendMessage("§a[MightyRTP] §7Download: §9https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID);
                                }
                            }
                        }, 100L); // 5 seconds delay (100 ticks = 5 seconds)
                    });
                } else {
                    // Plugin is up to date
                    if (isDebugEnabled()) {
                        getLogger().info("[DEBUG] Plugin is up to date (version " + currentVersion + ")");
                    }
                    
                    // Send "up to date" message to the player who requested the check
                    if (player != null) {
                        getServer().getScheduler().runTask(this, () -> {
                            player.sendMessage("§a[MightyRTP] §aPlugin is up to date (version " + currentVersion + ")");
                        });
                    }
                }
            } catch (Exception e) {
                if (isDebugEnabled()) {
                    getLogger().warning("Could not check for updates: " + e.getMessage());
                }
                
                // Send error message to the player who requested the check
                if (player != null) {
                    getServer().getScheduler().runTask(this, () -> {
                        player.sendMessage("§c[MightyRTP] Could not check for updates: " + e.getMessage());
                    });
                }
            }
        });
    }
    
    /**
     * Compare version strings to check if latest is newer
     */
    private boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) {
            if (isDebugEnabled()) {
                getLogger().info("[DEBUG] Version comparison failed - null values: latest=" + latest + ", current=" + current);
            }
            return false;
        }
        
        // Clean version strings - remove common prefixes like "Alpha", "Beta", "v", etc.
        String cleanLatest = latest.trim().replaceAll("^(v|version|alpha|beta|release)\\s*", "").trim();
        String cleanCurrent = current.trim().replaceAll("^(v|version|alpha|beta|release)\\s*", "").trim();
        
        // Also handle case variations
        cleanLatest = cleanLatest.replaceAll("^(Alpha|Beta|Release|V|Version)\\s*", "").trim();
        cleanCurrent = cleanCurrent.replaceAll("^(Alpha|Beta|Release|V|Version)\\s*", "").trim();
        
        if (isDebugEnabled()) {
            getLogger().info("[DEBUG] Comparing versions - Latest: '" + latest + "' -> '" + cleanLatest + "', Current: '" + current + "' -> '" + cleanCurrent + "'");
        }
        
        // Simple version comparison - you might want to use a proper version parser
        // This handles basic semantic versioning (1.0.5 vs 1.0.6)
        try {
            String[] latestParts = cleanLatest.split("\\.");
            String[] currentParts = cleanCurrent.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (isDebugEnabled()) {
                    getLogger().info("[DEBUG] Comparing part " + i + ": " + latestPart + " vs " + currentPart);
                }
                
                if (latestPart > currentPart) {
                    if (isDebugEnabled()) {
                        getLogger().info("[DEBUG] Latest version is newer at position " + i);
                    }
                    return true;
                } else if (latestPart < currentPart) {
                    if (isDebugEnabled()) {
                        getLogger().info("[DEBUG] Current version is newer at position " + i);
                    }
                    return false;
                }
            }
            
            if (isDebugEnabled()) {
                getLogger().info("[DEBUG] Versions are equal");
            }
            return false; // Versions are equal
        } catch (NumberFormatException e) {
            if (isDebugEnabled()) {
                getLogger().warning("[DEBUG] Version parsing failed, using string comparison: " + e.getMessage());
            }
            // If version parsing fails, do simple string comparison
            boolean result = !cleanLatest.equals(cleanCurrent);
            if (isDebugEnabled()) {
                getLogger().info("[DEBUG] String comparison result: " + result);
            }
            return result;
        }
    }
    
    /**
     * Migrate config file to add missing options from newer versions
     * Simple approach: Use default config structure/comments, merge in user's values
     * This ensures existing config files get new options added without losing custom settings
     * Preserves all comments and formatting from default config
     */
    public void migrateConfig() {
        try {
            // Get the actual config file
            File configFile = new File(getDataFolder(), "config.yml");
            
            if (!configFile.exists()) {
                return; // No migration needed if file doesn't exist
            }
            
            // Load the default config from the jar resource (get it twice - once for text, once for YAML)
            InputStream defaultConfigTextStream = getResource("config.yml");
            if (defaultConfigTextStream == null) {
                getLogger().warning("Could not load default config.yml from jar for migration");
                return;
            }
            
            // Read default config as text to preserve formatting
            List<String> currentLines = new ArrayList<>(Files.readAllLines(configFile.toPath()));
            List<String> defaultLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(defaultConfigTextStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    defaultLines.add(line);
                }
            }
            
            // Also load as YAML to check what's missing (get resource again - getResource returns new stream)
            InputStream defaultConfigYamlStream = getResource("config.yml");
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigYamlStream));
            YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // Check config version - get default version
            int defaultVersion = defaultConfig.getInt("config_version", 1);
            int currentVersion = currentConfig.getInt("config_version", 0); // 0 means old config without version
            
            // If versions match and config has version field, no migration needed
            if (currentVersion == defaultVersion && currentConfig.contains("config_version")) {
                return; // Config is up to date
            }
            
            // Simple merge approach: Use default config structure/comments, replace values with user's where they exist
            // This preserves all comments and formatting from default, while keeping user's custom values
            // Deprecated keys (in user config but not in default) are automatically removed
            List<String> mergedLines = mergeConfigs(defaultLines, currentConfig, defaultConfig);
            
            // Check for and log deprecated keys that were removed
            Set<String> deprecatedKeys = findDeprecatedKeys(currentConfig, defaultConfig);
            if (!deprecatedKeys.isEmpty()) {
                getLogger().info("Removed deprecated config keys: " + String.join(", ", deprecatedKeys));
            }
            
            // Update config version
            updateConfigVersion(mergedLines, defaultVersion, defaultLines, "config_version");
            
            // Write merged config
            Files.write(configFile.toPath(), mergedLines, StandardCharsets.UTF_8);
            
            getLogger().info("Config migration completed - merged with default config, preserving user values and all comments");
            
            // Reload config
            reloadConfig();
            if (configManager != null) {
                configManager.reloadConfig();
            }
        } catch (Exception e) {
            getLogger().warning("Error during config migration: " + e.getMessage());
            // Don't fail plugin startup if migration has issues
            e.printStackTrace();
        }
    }
    
    /**
     * Merge default config (with comments) with user config (with values)
     * Simple approach: Use default structure/comments, replace values with user's where they exist
     */
    private List<String> mergeConfigs(List<String> defaultLines, YamlConfiguration userConfig, YamlConfiguration defaultConfig) {
        List<String> merged = new ArrayList<>();
        
        // Track current path for nested keys - store both name and indent level
        Stack<Pair<String, Integer>> pathStack = new Stack<>();
        
        for (int i = 0; i < defaultLines.size(); i++) {
            String line = defaultLines.get(i);
            String trimmed = line.trim();
            int currentIndent = line.length() - trimmed.length();
            
            // Always preserve comments and blank lines
            if (trimmed.isEmpty() || line.startsWith("#")) {
                merged.add(line);
                continue;
            }
            
            // Pop sections we've left (based on indent level)
            while (!pathStack.isEmpty() && currentIndent <= pathStack.peek().getValue()) {
                pathStack.pop();
            }
            
            // Check if this is a list item (starts with -)
            if (trimmed.startsWith("-")) {
                // This is a list item - preserve as-is (lists are handled at the parent key level)
                merged.add(line);
                continue;
            }
            
            // Check if this is a key=value line
            if (trimmed.contains(":") && !trimmed.startsWith("#")) {
                int colonIndex = trimmed.indexOf(':');
                String keyPart = trimmed.substring(0, colonIndex).trim();
                String valuePart = trimmed.substring(colonIndex + 1).trim();
                
                // Build full path for nested keys
                StringBuilder fullPathBuilder = new StringBuilder();
                for (Pair<String, Integer> pathEntry : pathStack) {
                    if (fullPathBuilder.length() > 0) {
                        fullPathBuilder.append(".");
                    }
                    fullPathBuilder.append(pathEntry.getKey());
                }
                if (fullPathBuilder.length() > 0) {
                    fullPathBuilder.append(".");
                }
                fullPathBuilder.append(keyPart);
                String fullPath = fullPathBuilder.toString();
                
                // Check if this is a section (value is empty and next line is indented or is a list)
                boolean isSection = valuePart.isEmpty();
                boolean isList = false;
                if (isSection && i + 1 < defaultLines.size()) {
                    // Look ahead to see if next non-comment line is indented or is a list item
                    for (int j = i + 1; j < defaultLines.size() && j < i + 10; j++) {
                        String nextLine = defaultLines.get(j);
                        String nextTrimmed = nextLine.trim();
                        if (nextTrimmed.isEmpty() || nextLine.startsWith("#")) {
                            continue;
                        }
                        int nextIndent = nextLine.length() - nextTrimmed.length();
                        if (nextTrimmed.startsWith("-")) {
                            // This is a list
                            isList = true;
                            isSection = true;
                            break;
                        } else if (nextIndent > currentIndent) {
                            isSection = true;
                            break;
                        } else {
                            // Next line is at same or less indent - not a section
                            break;
                        }
                    }
                }
                
                if (isSection) {
                    // This is a section - check if user has values for it
                    if (userConfig.contains(fullPath)) {
                        Object userValue = userConfig.get(fullPath);
                        
                        // If it's a list, we need to handle it specially
                        if (isList && userValue instanceof List) {
                            // Add the key line
                            merged.add(line);
                            // Add list items from user config
                            List<?> userList = (List<?>) userValue;
                            for (Object item : userList) {
                                String itemStr = formatYamlValue(item);
                                // Remove quotes if they were added (lists often don't need them)
                                if (itemStr.startsWith("\"") && itemStr.endsWith("\"")) {
                                    itemStr = itemStr.substring(1, itemStr.length() - 1);
                                }
                                merged.add(" ".repeat(currentIndent + 2) + "- " + itemStr);
                            }
                            // Skip the default list items and their comments - we've already added user's
                            // Skip until we're out of the list (next line at same or less indent)
                            while (i + 1 < defaultLines.size()) {
                                String nextLine = defaultLines.get(i + 1);
                                String nextTrimmed = nextLine.trim();
                                int nextIndent = nextLine.length() - nextTrimmed.length();
                                
                                // If it's a comment or blank line within the list, skip it
                                if (nextTrimmed.isEmpty() || nextLine.startsWith("#")) {
                                    if (nextIndent > currentIndent) {
                                        i++; // Skip comment/blank within list
                                    } else {
                                        break; // Comment at section level or above - end of list
                                    }
                                } else if (nextTrimmed.startsWith("-") && nextIndent > currentIndent) {
                                    i++; // Skip this list item
                                } else {
                                    break; // End of list
                                }
                            }
                            pathStack.push(new Pair<>(keyPart, currentIndent));
                            continue;
                        } else {
                            // Regular section - add it and push to path stack
                            merged.add(line);
                            pathStack.push(new Pair<>(keyPart, currentIndent));
                        }
                    } else {
                        // User doesn't have this section - use default (with all comments and list items)
                        merged.add(line);
                        pathStack.push(new Pair<>(keyPart, currentIndent));
                    }
                } else {
                    // This is a key=value line
                    // Skip version keys - they're handled separately by updateConfigVersion
                    if (keyPart.equals("config_version") || keyPart.equals("messages_version")) {
                        // Use default version line - it will be updated by updateConfigVersion
                        merged.add(line);
                    } else if (userConfig.contains(fullPath)) {
                        // User has this key - use their value but keep default's formatting
                        Object userValue = userConfig.get(fullPath);
                        String userValueStr = formatYamlValue(userValue);
                        
                        // Preserve inline comment if present
                        String inlineComment = "";
                        int commentIndex = valuePart.indexOf('#');
                        if (commentIndex >= 0) {
                            inlineComment = " " + valuePart.substring(commentIndex);
                        }
                        
                        // Replace value while preserving indentation and inline comment
                        merged.add(" ".repeat(currentIndent) + keyPart + ": " + userValueStr + inlineComment);
                    } else {
                        // User doesn't have this key - use default (with default value and comments)
                        merged.add(line);
                    }
                }
            } else {
                // Not a key=value line - preserve as-is
                merged.add(line);
            }
        }
        
        return merged;
    }
    
    /**
     * Find deprecated keys that exist in user config but not in default config
     * These keys will be removed during migration
     */
    private Set<String> findDeprecatedKeys(YamlConfiguration userConfig, YamlConfiguration defaultConfig) {
        Set<String> deprecated = new HashSet<>();
        findDeprecatedKeysRecursive(userConfig, defaultConfig, "", deprecated);
        return deprecated;
    }
    
    /**
     * Recursively find deprecated keys
     */
    private void findDeprecatedKeysRecursive(YamlConfiguration userConfig, YamlConfiguration defaultConfig, 
                                             String basePath, Set<String> deprecated) {
        for (String key : userConfig.getKeys(false)) {
            String fullPath = basePath.isEmpty() ? key : basePath + "." + key;
            
            // Skip version keys - they're handled separately
            if (key.equals("config_version") || key.equals("messages_version")) {
                continue;
            }
            
            if (!defaultConfig.contains(fullPath)) {
                // This key doesn't exist in default config - it's deprecated
                deprecated.add(fullPath);
            } else if (userConfig.isConfigurationSection(key) && defaultConfig.isConfigurationSection(fullPath)) {
                // Both are sections - recursively check nested keys
                findDeprecatedKeysRecursive(
                    userConfig.getConfigurationSection(key),
                    defaultConfig.getConfigurationSection(fullPath),
                    fullPath,
                    deprecated
                );
            }
        }
    }
    
    /**
     * Recursive helper for configuration sections
     */
    private void findDeprecatedKeysRecursive(org.bukkit.configuration.ConfigurationSection userSection,
                                            org.bukkit.configuration.ConfigurationSection defaultSection,
                                            String basePath, Set<String> deprecated) {
        for (String key : userSection.getKeys(false)) {
            String fullPath = basePath.isEmpty() ? key : basePath + "." + key;
            
            // Skip version keys - they're handled separately
            if (key.equals("config_version") || key.equals("messages_version")) {
                continue;
            }
            
            if (!defaultSection.contains(key)) {
                // This key doesn't exist in default config - it's deprecated
                deprecated.add(fullPath);
            } else if (userSection.isConfigurationSection(key) && defaultSection.isConfigurationSection(key)) {
                // Both are sections - recursively check nested keys
                findDeprecatedKeysRecursive(
                    userSection.getConfigurationSection(key),
                    defaultSection.getConfigurationSection(key),
                    fullPath,
                    deprecated
                );
            }
        }
    }
    
    // Simple Pair class for path tracking
    private static class Pair<K, V> {
        private final K key;
        private final V value;
        
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
    
    /**
     * Format a YAML value as a string
     */
    private String formatYamlValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            // Check if it needs quotes
            String str = (String) value;
            if (str.contains(":") || str.contains("#") || str.trim().isEmpty() || 
                str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false") || 
                str.equalsIgnoreCase("null") || str.matches("^-?\\d+$")) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return str;
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof List) {
            // Format list
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            // For lists, we'll just return the first approach - inline if simple
            if (list.size() == 1 && (list.get(0) instanceof String || list.get(0) instanceof Number)) {
                return "[" + formatYamlValue(list.get(0)) + "]";
            }
            // Multi-line list - return as inline for now, could be improved
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatYamlValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return value.toString();
        }
    }
    
    /**
     * Migrate a specific config file (messages.yml, etc.)
     * Uses the same simple merge approach as config.yml
     */
    private void migrateConfigFile(String filename) {
        try {
            File configFile = new File(getDataFolder(), filename);
            
            // Save default config if it doesn't exist
            if (!configFile.exists()) {
                saveResource(filename, false);
                return;
            }
            
            // Load default config from jar
            InputStream defaultConfigTextStream = getResource(filename);
            if (defaultConfigTextStream == null) {
                return; // No default file in jar, skip
            }
            
            // Read default config as text to preserve formatting
            List<String> defaultLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(defaultConfigTextStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    defaultLines.add(line);
                }
            }
            
            // Load as YAML to get user values
            InputStream defaultConfigYamlStream = getResource(filename);
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigYamlStream));
            YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // Check config version - get default version (use filename + "_version" as key)
            String versionKey = filename.replace(".yml", "_version");
            int defaultVersion = defaultConfig.getInt(versionKey, 1);
            int currentVersion = currentConfig.getInt(versionKey, 0); // 0 means old config without version
            
            // If versions match and config has version field, no migration needed
            if (currentVersion == defaultVersion && currentConfig.contains(versionKey)) {
                return; // Config is up to date
            }
            
            // Simple merge: Use default structure/comments, replace values with user's where they exist
            // Deprecated keys (in user config but not in default) are automatically removed
            List<String> mergedLines = mergeConfigs(defaultLines, currentConfig, defaultConfig);
            
            // Check for and log deprecated keys that were removed
            Set<String> deprecatedKeys = findDeprecatedKeys(currentConfig, defaultConfig);
            if (!deprecatedKeys.isEmpty()) {
                getLogger().info("Removed deprecated keys from " + filename + ": " + String.join(", ", deprecatedKeys));
            }
            
            // Update config version
            updateConfigVersion(mergedLines, defaultVersion, defaultLines, versionKey);
            
            // Write merged config
            Files.write(configFile.toPath(), mergedLines, StandardCharsets.UTF_8);
            
            if (isDebugEnabled()) {
                getLogger().info("Migrated " + filename + " - merged with default, preserving user values and all comments");
            }
            
        } catch (Exception e) {
            if (isDebugEnabled()) {
                getLogger().warning("Error migrating " + filename + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Update version field in the config file lines
     * Preserves comments and formatting
     * Extracts comment from default config if adding new
     */
    private void updateConfigVersion(List<String> lines, int newVersion, List<String> defaultLines, String versionKey) {
        // Look for version line and update it, or add it if missing
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            // Check if this is the version line
            if (trimmed.startsWith(versionKey + ":") || trimmed.startsWith(versionKey + " ")) {
                // Update the version value, preserving indentation and any inline comments
                int indent = line.length() - trimmed.length();
                String restOfLine = "";
                int colonIndex = trimmed.indexOf(':');
                if (colonIndex >= 0 && colonIndex + 1 < trimmed.length()) {
                    String afterColon = trimmed.substring(colonIndex + 1).trim();
                    // Check if there's an inline comment
                    int commentIndex = afterColon.indexOf('#');
                    if (commentIndex >= 0) {
                        restOfLine = " #" + afterColon.substring(commentIndex + 1);
                    }
                }
                lines.set(i, " ".repeat(indent) + versionKey + ": " + newVersion + restOfLine);
                found = true;
                break;
            }
        }
        
        // If not found, add it after the header comment (usually line 2-3)
        if (!found) {
            // Extract comment from default config
            String commentLine = "# Config version - do not modify";
            for (int i = 0; i < defaultLines.size(); i++) {
                String line = defaultLines.get(i);
                String trimmed = line.trim();
                // Look for version key in default config
                if (trimmed.startsWith(versionKey + ":") || trimmed.startsWith(versionKey + " ")) {
                    // Check if there's a comment line before it
                    if (i > 0) {
                        String prevLine = defaultLines.get(i - 1);
                        if (prevLine.trim().startsWith("#")) {
                            commentLine = prevLine;
                        }
                    }
                    break;
                }
            }
            
            int insertIndex = 0;
            // Find a good place to insert - after first comment block, before first section
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                // Stop at first non-comment, non-blank line that's not the version key
                if (!trimmed.isEmpty() && !line.startsWith("#") && !trimmed.startsWith(versionKey)) {
                    insertIndex = i;
                    break;
                }
            }
            // Insert version line with comment from default config
            lines.add(insertIndex, commentLine);
            lines.add(insertIndex + 1, versionKey + ": " + newVersion);
            // Add blank line after if needed
            if (insertIndex + 2 < lines.size() && !lines.get(insertIndex + 2).trim().isEmpty()) {
                lines.add(insertIndex + 2, "");
            }
        }
    }
    
    /**
     * Handle player join event - notify OP'd players about available updates
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Notify OP'd players about available updates with a delay to show after MOTD
        if (updateAvailable && event.getPlayer().isOp()) {
            String currentVersion = getDescription().getVersion();
            getServer().getScheduler().runTaskLater(this, () -> {
                event.getPlayer().sendMessage("§a[MightyRTP] §eUpdate available: §f" + latestVersion + " §7(current: " + currentVersion + ")");
                event.getPlayer().sendMessage("§a[MightyRTP] §7Download: §9https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID);
            }, 100L); // 5 seconds delay (100 ticks = 5 seconds)
        }
    }
}
