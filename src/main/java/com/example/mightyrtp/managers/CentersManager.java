package com.example.mightyrtp.managers;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class CentersManager {
    
    private final MightyRTP plugin;
    private FileConfiguration centersConfig;
    private File centersFile;
    
    public CentersManager(MightyRTP plugin) {
        this.plugin = plugin;
        this.centersFile = new File(plugin.getDataFolder(), "centers.yml");
    }
    
    /**
     * Load or create the centers.yml file
     */
    public void loadCenters() {
        if (!centersFile.exists()) {
            // Save default centers.yml from resources
            plugin.saveResource("centers.yml", false);
        }
        
        centersConfig = YamlConfiguration.loadConfiguration(centersFile);
        
        // Initialize centers for all loaded worlds (if not already set)
        initializeWorldCenters();
    }
    
    /**
     * Reload the centers.yml file
     */
    public void reloadCenters() {
        centersConfig = YamlConfiguration.loadConfiguration(centersFile);
        // Re-initialize centers for any newly loaded worlds
        initializeWorldCenters();
    }
    
    /**
     * Initialize centers for all loaded worlds that don't have one set
     * Sets default center to (0, 0) for each world
     */
    private void initializeWorldCenters() {
        boolean needsSave = false;
        
        for (World world : plugin.getServer().getWorlds()) {
            String worldName = world.getName();
            String xPath = "centers." + worldName + ".x";
            String zPath = "centers." + worldName + ".z";
            
            // Only set if not already configured
            if (!centersConfig.contains(xPath) || !centersConfig.contains(zPath)) {
                centersConfig.set(xPath, 0);
                centersConfig.set(zPath, 0);
                needsSave = true;
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Initialized RTP center for world '" + worldName + "' at (0, 0)");
                }
            }
        }
        
        if (needsSave) {
            saveCenters();
        }
    }
    
    /**
     * Save the centers.yml file
     */
    public void saveCenters() {
        try {
            centersConfig.save(centersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save centers.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the center location for a world
     * Returns default (0, 0) if not set
     */
    public Location getCenter(World world) {
        if (centersConfig == null) {
            loadCenters();
        }
        
        String worldName = world.getName();
        int x = centersConfig.getInt("centers." + worldName + ".x", 0);
        int z = centersConfig.getInt("centers." + worldName + ".z", 0);
        
        return new Location(world, x, 0, z);
    }
    
    /**
     * Set the center location for a world
     */
    public void setCenter(World world, int x, int z) {
        if (centersConfig == null) {
            loadCenters();
        }
        
        String worldName = world.getName();
        centersConfig.set("centers." + worldName + ".x", x);
        centersConfig.set("centers." + worldName + ".z", z);
        saveCenters();
    }
    
    /**
     * Get the center X coordinate for a world
     */
    public int getCenterX(World world) {
        if (centersConfig == null) {
            loadCenters();
        }
        
        String worldName = world.getName();
        return centersConfig.getInt("centers." + worldName + ".x", 0);
    }
    
    /**
     * Get the center Z coordinate for a world
     */
    public int getCenterZ(World world) {
        if (centersConfig == null) {
            loadCenters();
        }
        
        String worldName = world.getName();
        return centersConfig.getInt("centers." + worldName + ".z", 0);
    }
}

