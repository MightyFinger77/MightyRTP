package com.example.mightyrtp.utils;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.CentersManager;
import com.example.mightyrtp.managers.SpotsManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TeleportUtils {
    
    private final MightyRTP plugin;
    private final ConfigManager configManager;
    private final CentersManager centersManager;
    private final SpotsManager spotsManager;
    private final Random random;
    
    public TeleportUtils(MightyRTP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.centersManager = plugin.getCentersManager();
        this.spotsManager = plugin.getSpotsManager();
        this.random = new Random();
    }
    
    /**
     * Asynchronously find a safe location and call the callback when done
     * This prevents blocking the main server thread
     */
    public void findSafeLocationAsync(World world, Location center, Player player, Consumer<TeleportResult> callback) {
        // Run the heavy location finding on an async task
        new BukkitRunnable() {
            @Override
            public void run() {
                TeleportResult result = findSafeLocationSync(world, center);
                
                // Sync back to main thread to execute the callback
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(result);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Synchronous version of findSafeLocation (runs on async thread)
     * This is the heavy computation that was previously blocking the main thread
     */
    private TeleportResult findSafeLocationSync(World world, Location center) {
        if (world == null) {
            return TeleportResult.failure("World is null");
        }

        // Check if custom teleport mode is enabled
        if (configManager.isCustomTeleportMode()) {
            Location customSpot = spotsManager.getRandomSpot(world);
            if (customSpot != null) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("[MightyRTP] Using custom teleport spot at " + customSpot.getBlockX() + ", " + customSpot.getBlockY() + ", " + customSpot.getBlockZ());
                }
                return TeleportResult.success(customSpot);
            } else {
                // No custom spots available, return failure
                return TeleportResult.failure("No custom teleport spots available for world: " + world.getName() + ". Use /rtp-set to add spots.");
            }
        }

        int maxAttempts = configManager.getMaxAttempts();
        int teleportDistance = configManager.getTeleportDistance();
        int minDistanceFromSpawn = configManager.getMinDistanceFromSpawn();
        boolean debugEnabled = configManager.isDebugEnabled();
        int debugLogInterval = configManager.getDebugLogAttemptInterval();

        // Get center from CentersManager (defaults to 0,0 if not set)
        int centerX = centersManager.getCenterX(world);
        int centerZ = centersManager.getCenterZ(world);

        // Fast mode: extremely aggressive for console commands
        int finalMaxAttempts = maxAttempts;
        
        if (configManager.isFastModeEnabled()) {
            finalMaxAttempts = Math.min(finalMaxAttempts, configManager.getFastModeMaxAttempts());
        }

        // Fast strategy: try to find ANY valid location with minimal checks
        for (int attempt = 1; attempt <= finalMaxAttempts; attempt++) {
            // Generate random coordinates within the teleport distance
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * teleportDistance;
            
            int x = centerX + (int) (Math.cos(angle) * distance);
            int z = centerZ + (int) (Math.sin(angle) * distance);

            // Check minimum distance from center
            double distanceFromCenter = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
            if (distanceFromCenter < minDistanceFromSpawn) {
                continue;
            }

            // Fast mode: skip chunk loading entirely, just try to find a location
            if (configManager.isFastModeEnabled()) {
                // Try to find the highest block without loading chunks
                int highestY = findHighestSolidBlockFast(world, x, z);
                if (highestY != -1) {
                    // Fast mode: minimal safety checks
                    if (isLocationSafeFast(world, x, highestY, z)) {
                        if (debugEnabled) {
                            plugin.getLogger().info("[MightyRTP] Found fast location at x=" + x + ", z=" + z + ", y=" + highestY + " after " + attempt + " attempts");
                        }
                        int adjustedY = highestY + 1;
                        Location location = new Location(world, x, adjustedY, z);
                        return TeleportResult.success(location);
                    }
                }
                continue;
            }

            // Normal mode: full chunk loading and safety checks
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                try {
                    CompletableFuture<org.bukkit.Chunk> chunkFuture = world.getChunkAtAsync(x >> 4, z >> 4, true);
                    org.bukkit.Chunk chunk = chunkFuture.get(1, TimeUnit.SECONDS);
                    
                    if (chunk == null || !chunk.isLoaded()) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            // Find the highest solid block at this X,Z coordinate
            int highestY = findHighestSolidBlockSmart(world, x, z);
            if (highestY == -1) {
                continue;
            }

            if (isLocationSafe(world, x, highestY, z, configManager.getUnsafeBlocks())) {
                if (debugEnabled) {
                    plugin.getLogger().info("[MightyRTP] Found safe location at x=" + x + ", z=" + z + ", y=" + highestY + " after " + attempt + " attempts");
                }
                int adjustedY = highestY + 1;
                Location location = new Location(world, x, adjustedY, z);
                return TeleportResult.success(location);
            }
        }

        if (debugEnabled) {
            plugin.getLogger().info("[MightyRTP] Failed to find a safe location after " + finalMaxAttempts + " attempts in world: " + world.getName());
        }
        
        // Fast fallback: try spawn area with minimal checks
        if (configManager.isFastModeEnabled()) {
            for (int attempt = 1; attempt <= 3; attempt++) {
                // Try closer to spawn for fast mode
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * 1000; // Much smaller radius
                
                int x = centerX + (int) (Math.cos(angle) * distance);
                int z = centerZ + (int) (Math.sin(angle) * distance);

                double distanceFromCenter = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (distanceFromCenter < minDistanceFromSpawn) {
                    continue;
                }

                int highestY = findHighestSolidBlockFast(world, x, z);
                if (highestY != -1 && isLocationSafeFast(world, x, highestY, z)) {
                    if (debugEnabled) {
                        plugin.getLogger().info("[MightyRTP] Found fast fallback location at x=" + x + ", z=" + z + ", y=" + highestY);
                    }
                    int adjustedY = highestY + 1;
                    Location location = new Location(world, x, adjustedY, z);
                    return TeleportResult.success(location);
                }
            }
        }
        
        return TeleportResult.failure("Could not find a safe location after " + finalMaxAttempts + " attempts");
    }
    
    /**
     * Legacy synchronous method for backward compatibility
     * @deprecated Use findSafeLocationAsync instead to avoid blocking the main thread
     */
    @Deprecated
    public TeleportResult findSafeLocation(World world, Location center) {
        return findSafeLocationSync(world, center);
    }
    
    private boolean isLocationSafe(World world, int x, int y, int z, List<Material> unsafeBlocks) {
        try {
            boolean isNether = world.getEnvironment() == World.Environment.NETHER;
            
            // Check the block at the teleport location
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockBelow = world.getBlockAt(x, y - 1, z);
            
            // Check if the block below is explicitly unsafe (like lava, fire, etc.)
            // For Nether: be more lenient - only check for immediately dangerous blocks below
            if (isNether) {
                // In Nether, only reject if block below is fire or magma block (immediately dangerous)
                if (blockBelow.getType() == Material.FIRE || blockBelow.getType() == Material.MAGMA_BLOCK) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Block below is immediately dangerous in Nether - " + blockBelow.getType() + " at y=" + (y-1));
                    }
                    return false;
                }
            } else {
                // In other dimensions, use the full unsafe blocks check
                if (unsafeBlocks.contains(blockBelow.getType())) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Block below is explicitly unsafe - " + blockBelow.getType() + " at y=" + (y-1));
                    }
                    return false;
                }
            }
            
            // The teleport location can be a solid block (like grass, leaves, etc.) - players can stand on it
            // We just need to make sure there's enough air above for the player to fit
            
            int safetyStrictness = configManager.getSafetyStrictness();
            
            if (safetyStrictness <= 2) {
                // Very strict (1-2): Only teleport to solid blocks with 2 blocks of air above
                if (block.getType() == Material.AIR || block.getType().name().contains("LEAVES")) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Very strict mode - cannot teleport to air or leaves");
                    }
                    return false;
                }
                
                // Need 2 blocks of air above
                if (blockAbove.getType() != Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Very strict mode - not enough air space above");
                    }
                    return false;
                }
                
                if (y + 2 < world.getMaxHeight()) {
                    Block blockAbove2 = world.getBlockAt(x, y + 2, z);
                    if (blockAbove2.getType() != Material.AIR) {
                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("Location rejected: Very strict mode - not enough vertical space");
                        }
                        return false;
                    }
                }
            } else if (safetyStrictness >= 4) {
                // Very lenient (4-5): Teleport to any solid block with 1 block of air above
                if (block.getType() == Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Very lenient mode - cannot teleport to air");
                    }
                    return false;
                }
                
                if (blockAbove.getType() != Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Very lenient mode - not enough air space above");
                    }
                    return false;
                }
            } else {
                // Normal (3): Teleport to solid blocks or leaves with 2 blocks of air above
                if (block.getType() == Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Normal mode - cannot teleport to air");
                    }
                    return false;
                }
                
                if (blockAbove.getType() != Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Location rejected: Normal mode - not enough air space above");
                    }
                    return false;
                }
                
                if (y + 2 < world.getMaxHeight()) {
                    Block blockAbove2 = world.getBlockAt(x, y + 2, z);
                    if (blockAbove2.getType() != Material.AIR) {
                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("Location rejected: Normal mode - not enough vertical space");
                        }
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            // If any exception occurs during block checking, reject this location
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Location rejected: Exception during safety check at x=" + x + ", z=" + z + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Find the highest solid block at the given X,Z coordinates using smart scanning
     * Starts from reasonable heights and works down for better performance
     * Returns -1 if no solid block is found
     */
    private int findHighestSolidBlockSmart(World world, int x, int z) {
        try {
            // Nether-specific location finding - scan from Y=32 to Y=100
            if (world.getEnvironment() == World.Environment.NETHER) {
                return findSuitableLocationNether(world, x, z);
            }
            
            // Get dimension-specific height limits for other dimensions
            int maxHeight = getMaxSearchHeight(world);
            int minHeight = getMinSearchHeight(world);
            
            // Start from a reasonable height and work down
            for (int y = maxHeight; y >= minHeight; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR && block.getType() != Material.VOID_AIR) {
                    return y;
                }
            }
            return -1; // No solid block found
        } catch (Exception e) {
            return -1; // Return -1 on any exception
        }
    }

    /**
     * Find the highest solid block at the given X,Z coordinates using ultra-fast mode
     * This is a simplified version that skips chunk loading and assumes a high chance of success
     * Returns -1 if no solid block is found
     */
    private int findHighestSolidBlockFast(World world, int x, int z) {
        try {
            // Nether-specific location finding - scan from Y=32 to Y=100
            if (world.getEnvironment() == World.Environment.NETHER) {
                return findSuitableLocationNether(world, x, z);
            }
            
            // Get dimension-specific height limits for other dimensions
            int maxHeight = getMaxSearchHeight(world);
            int minHeight = getMinSearchHeight(world);
            
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[MightyRTP] Searching for solid block at x=" + x + ", z=" + z + " from y=" + maxHeight + " to y=" + minHeight + " in " + world.getEnvironment());
            }
            
            // Start from the max height and work down
            for (int y = maxHeight; y >= minHeight; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR && block.getType() != Material.VOID_AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("[MightyRTP] Found solid block " + block.getType() + " at y=" + y);
                    }
                    return y;
                }
            }
            
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[MightyRTP] No solid block found at x=" + x + ", z=" + z);
            }
            return -1; // No solid block found
        } catch (Exception e) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[MightyRTP] Exception finding solid block at x=" + x + ", z=" + z + ": " + e.getMessage());
            }
            return -1; // Return -1 on any exception
        }
    }

    /**
     * Fast safety check for locations found by findHighestSolidBlockFast
     * This is a simplified version that still checks for dangerous blocks but skips complex validation
     * Returns true if the location is safe, false otherwise
     */
    private boolean isLocationSafeFast(World world, int x, int y, int z) {
        try {
            int safetyLevel = configManager.getFastModeSafetyLevel();
            boolean isNether = world.getEnvironment() == World.Environment.NETHER;
            
            // Fast mode: still check for dangerous blocks but skip complex validation
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockBelow = world.getBlockAt(x, y - 1, z);
            
            // Level 1: Basic unsafe blocks check using config
            if (safetyLevel >= 1) {
                List<Material> unsafeBlocks = configManager.getUnsafeBlocks();
                
                // Check if the teleport location is in the unsafe blocks list
                if (unsafeBlocks.contains(block.getType())) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("[MightyRTP] Location rejected: Teleport block is unsafe - " + block.getType());
                    }
                    return false;
                }
                
                // For Nether: be more lenient - only check block below if it's not lava
                // In Nether, it's common to have lava below solid blocks, which is safe to stand on
                if (isNether) {
                    // In Nether, only reject if block below is fire or other immediately dangerous blocks
                    if (blockBelow.getType() == Material.FIRE || blockBelow.getType() == Material.MAGMA_BLOCK) {
                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("[MightyRTP] Location rejected: Block below is immediately dangerous in Nether - " + blockBelow.getType());
                        }
                        return false;
                    }
                } else {
                    // In other dimensions, use the full unsafe blocks check
                    if (unsafeBlocks.contains(blockBelow.getType())) {
                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("[MightyRTP] Location rejected: Block below is unsafe - " + blockBelow.getType());
                        }
                        return false;
                    }
                }
            }
            
            // Level 2: Standard safety (unsafe blocks + air above)
            if (safetyLevel >= 2) {
                // Check if there's enough air above (at least 1 block)
                if (blockAbove.getType() != Material.AIR) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("[MightyRTP] Location rejected: No air above - " + blockAbove.getType());
                    }
                    return false;
                }
            }
            
            // Level 3: Full safety (includes additional checks)
            if (safetyLevel >= 3) {
                // Additional safety checks can be added here
                // For now, we already have the unsafe blocks check from level 1
            }
            
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[MightyRTP] Location accepted: Safe location found at x=" + x + ", y=" + y + ", z=" + z + " (block=" + block.getType() + ", above=" + blockAbove.getType() + ", below=" + blockBelow.getType() + ")");
            }
            
            return true;
        } catch (Exception e) {
            // If any exception occurs during fast safety check, reject this location
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Location rejected: Fast safety check failed at x=" + x + ", z=" + z + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Get the maximum search height for a given world dimension
     * This prevents scanning above the Nether roof and other dimension limits
     */
    private int getMaxSearchHeight(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            // In the Nether, don't search above Y=120 to avoid the roof
            return 120;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            // In the End, search up to a reasonable height
            return 100;
        } else {
            // Overworld and other dimensions - use a reasonable height
            return 120;
        }
    }
    
    /**
     * Get the minimum search height for a given world dimension
     */
    private int getMinSearchHeight(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            // In the Nether, start from Y=8 (above bedrock floor, more Nether terrain is lower)
            return 8;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            // In the End, start from Y=0
            return 0;
        } else {
            // Overworld and other dimensions - start from Y=32
            return 32;
        }
    }
    
    /**
     * Nether-specific location finding algorithm
     * Scans from Y=32 to Y=100 to find suitable locations, avoiding the roof
     * Returns the Y coordinate of a suitable location, or -1 if none found
     */
    private int findSuitableLocationNether(World world, int x, int z) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("[MightyRTP] Searching for suitable Nether location at x=" + x + ", z=" + z + " from y=32 to y=100");
        }
        
        // Scan from Y=32 to Y=100 (avoiding the roof at Y=127)
        for (int y = 32; y <= 100; y++) {
            if (isSuitableLocationNether(world, x, y, z)) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("[MightyRTP] Found suitable Nether location at x=" + x + ", y=" + y + ", z=" + z);
                }
                return y;
            }
        }
        
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("[MightyRTP] No suitable Nether location found at x=" + x + ", z=" + z);
        }
        return -1;
    }
    
    /**
     * Check if a location in the Nether is suitable for teleportation
     * Requirements:
     * - Standing block is solid and not magma block
     * - Two blocks of air above for player space
     */
    private boolean isSuitableLocationNether(World world, int x, int y, int z) {
        try {
            Block standingBlock = world.getBlockAt(x, y, z);
            Block aboveBlock = world.getBlockAt(x, y + 1, z);
            Block twoAboveBlock = world.getBlockAt(x, y + 2, z);
            
            // Check if standing block is solid and not magma block
            if (!standingBlock.getType().isSolid() || standingBlock.getType() == Material.MAGMA_BLOCK) {
                return false;
            }
            
            // Check if there are two blocks of air above
            if (aboveBlock.getType() != Material.AIR || twoAboveBlock.getType() != Material.AIR) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[MightyRTP] Exception checking Nether location suitability at x=" + x + ", y=" + y + ", z=" + z + ": " + e.getMessage());
            }
            return false;
        }
    }
}
