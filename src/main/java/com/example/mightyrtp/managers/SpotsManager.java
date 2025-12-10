package com.example.mightyrtp.managers;

import com.example.mightyrtp.MightyRTP;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpotsManager {
    
    private final MightyRTP plugin;
    private FileConfiguration spotsConfig;
    private File spotsFile;
    private final Random random;
    
    public SpotsManager(MightyRTP plugin) {
        this.plugin = plugin;
        this.spotsFile = new File(plugin.getDataFolder(), "spots.yml");
        this.random = new Random();
    }
    
    /**
     * Load or create the spots.yml file
     */
    public void loadSpots() {
        if (!spotsFile.exists()) {
            // Save default spots.yml from resources
            plugin.saveResource("spots.yml", false);
        }
        
        spotsConfig = YamlConfiguration.loadConfiguration(spotsFile);
    }
    
    /**
     * Reload the spots.yml file
     */
    public void reloadSpots() {
        spotsConfig = YamlConfiguration.loadConfiguration(spotsFile);
    }
    
    /**
     * Save the spots.yml file
     */
    public void saveSpots() {
        try {
            spotsConfig.save(spotsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spots.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add a teleport spot for a world
     */
    public void addSpot(World world, Location location) {
        if (spotsConfig == null) {
            loadSpots();
        }
        
        String worldName = world.getName();
        List<Location> spots = getSpots(world);
        
        // Add the new spot
        spots.add(location);
        
        // Save all spots for this world
        saveSpotsForWorld(world, spots);
    }
    
    /**
     * Get all teleport spots for a world
     */
    public List<Location> getSpots(World world) {
        if (spotsConfig == null) {
            loadSpots();
        }
        
        String worldName = world.getName();
        List<Location> spots = new ArrayList<>();
        
        List<?> spotsList = spotsConfig.getList("spots." + worldName, new ArrayList<>());
        for (Object spotObj : spotsList) {
            if (spotObj instanceof org.bukkit.configuration.ConfigurationSection) {
                org.bukkit.configuration.ConfigurationSection spot = (org.bukkit.configuration.ConfigurationSection) spotObj;
                int x = spot.getInt("x");
                int y = spot.getInt("y");
                int z = spot.getInt("z");
                spots.add(new Location(world, x, y, z));
            }
        }
        
        return spots;
    }
    
    /**
     * Get a random teleport spot for a world
     * Returns null if no spots are available
     */
    public Location getRandomSpot(World world) {
        List<Location> spots = getSpots(world);
        
        if (spots.isEmpty()) {
            return null;
        }
        
        return spots.get(random.nextInt(spots.size()));
    }
    
    /**
     * Remove a spot at a specific location (within 1 block tolerance)
     */
    public boolean removeSpot(World world, Location location) {
        if (spotsConfig == null) {
            loadSpots();
        }
        
        List<Location> spots = getSpots(world);
        boolean removed = false;
        
        // Find and remove spots within 1 block of the location
        List<Location> toRemove = new ArrayList<>();
        for (Location spot : spots) {
            if (spot.getWorld().equals(world) &&
                Math.abs(spot.getBlockX() - location.getBlockX()) <= 1 &&
                Math.abs(spot.getBlockY() - location.getBlockY()) <= 1 &&
                Math.abs(spot.getBlockZ() - location.getBlockZ()) <= 1) {
                toRemove.add(spot);
                removed = true;
            }
        }
        
        spots.removeAll(toRemove);
        saveSpotsForWorld(world, spots);
        
        return removed;
    }
    
    /**
     * Get the number of spots for a world
     */
    public int getSpotCount(World world) {
        return getSpots(world).size();
    }
    
    /**
     * Save spots for a specific world
     */
    private void saveSpotsForWorld(World world, List<Location> spots) {
        String worldName = world.getName();
        List<java.util.Map<String, Object>> spotsData = new ArrayList<>();
        
        for (Location spot : spots) {
            java.util.Map<String, Object> spotData = new java.util.HashMap<>();
            spotData.put("x", spot.getBlockX());
            spotData.put("y", spot.getBlockY());
            spotData.put("z", spot.getBlockZ());
            spotsData.add(spotData);
        }
        
        spotsConfig.set("spots." + worldName, spotsData);
        saveSpots();
    }
}

