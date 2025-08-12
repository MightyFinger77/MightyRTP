package com.example.mightyrtp.utils;

import com.example.mightyrtp.MightyRTP;
import com.example.mightyrtp.managers.ConfigManager;
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
    private final Random random;
    
    public TeleportUtils(MightyRTP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
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

        int maxAttempts = configManager.getMaxAttempts();
        int teleportDistance = configManager.getTeleportDistance();
        int minDistanceFromSpawn = configManager.getMinDistanceFromSpawn();
        boolean debugEnabled = configManager.isDebugEnabled();
        int debugLogInterval = configManager.getDebugLogAttemptInterval();

        // Use (0,0) as the center reference point
        int centerX = 0;
        int centerZ = 0;

        // Single aggressive search strategy - try to load chunks as needed
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Generate random coordinates within the teleport distance
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * teleportDistance;
            
            int x = centerX + (int) (Math.cos(angle) * distance);
            int z = centerZ + (int) (Math.sin(angle) * distance);

            // Check minimum distance from spawn
            double distanceFromSpawn = Math.sqrt(x * x + z * z);
            if (distanceFromSpawn < minDistanceFromSpawn) {
                if (debugEnabled && attempt % debugLogInterval == 0) {
                    plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Location at x=" + x + ", z=" + z + " is too close to spawn, skipping...");
                }
                continue;
            }

            // Check if the chunk is loaded, if not, try to load it asynchronously
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                if (debugEnabled && attempt % debugLogInterval == 0) {
                    plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Chunk not loaded at x=" + x + ", z=" + z + ", attempting to load...");
                }
                
                // Use getChunkAtAsync instead of loadChunk to avoid AsyncCatcher errors
                try {
                    CompletableFuture<org.bukkit.Chunk> chunkFuture = world.getChunkAtAsync(x >> 4, z >> 4, true);
                    
                    // Wait for the chunk to load with a timeout
                    org.bukkit.Chunk chunk = chunkFuture.get(3, TimeUnit.SECONDS);
                    
                    // Verify the chunk was actually loaded successfully
                    if (chunk == null || !chunk.isLoaded()) {
                        if (debugEnabled && attempt % debugLogInterval == 0) {
                            plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Chunk load returned null or failed to load at x=" + x + ", z=" + z);
                        }
                        continue;
                    }
                    
                    if (debugEnabled && attempt % debugLogInterval == 0) {
                        plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Successfully loaded chunk at x=" + x + ", z=" + z);
                    }
                } catch (Exception e) {
                    if (debugEnabled && attempt % debugLogInterval == 0) {
                        plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Failed to load chunk at x=" + x + ", z=" + z + ": " + e.getMessage());
                    }
                    continue;
                }
            }

            // Find the highest solid block at this X,Z coordinate using smart scanning
            int highestY = findHighestSolidBlockSmart(world, x, z);
            if (highestY == -1) {
                if (debugEnabled && attempt % debugLogInterval == 0) {
                    plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": No solid block found at x=" + x + ", z=" + z + ", skipping...");
                }
                continue;
            }

            // Check if the location at the highest solid block is safe
            if (debugEnabled && attempt % debugLogInterval == 0) {
                plugin.getLogger().info("[MightyRTP] Attempt " + attempt + ": Checking location at x=" + x + ", z=" + z + ", y=" + highestY + "...");
            }

            if (isLocationSafe(world, x, highestY, z, configManager.getSafeBlocks(), configManager.getUnsafeBlocks())) {
                if (debugEnabled) {
                    plugin.getLogger().info("[MightyRTP] Found safe location at x=" + x + ", z=" + z + ", y=" + highestY + " after " + attempt + " attempts");
                }
                // Adjust Y position to place player on top of the block, not inside it
                int adjustedY = highestY + 1;
                Location location = new Location(world, x, adjustedY, z);
                return TeleportResult.success(location);
            }
        }

        if (debugEnabled) {
            plugin.getLogger().info("[MightyRTP] Failed to find a safe location after " + maxAttempts + " attempts in world: " + world.getName());
        }
        
        // Final aggressive fallback: try to load chunks in a smaller radius
        if (debugEnabled) {
            plugin.getLogger().info("[MightyRTP] Trying aggressive fallback - loading chunks in smaller radius...");
        }
        
        for (int attempt = 1; attempt <= 30; attempt++) {
            // Generate random coordinates within a much smaller radius
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * 1000; // Much smaller radius
            
            int x = centerX + (int) (Math.cos(angle) * distance);
            int z = centerZ + (int) (Math.sin(angle) * distance);

            // Check minimum distance from spawn
            double distanceFromSpawn = Math.sqrt(x * x + z * z);
            if (distanceFromSpawn < minDistanceFromSpawn) {
                continue;
            }

            // Try to load the chunk if it's not loaded using async method
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                try {
                    CompletableFuture<org.bukkit.Chunk> chunkFuture = world.getChunkAtAsync(x >> 4, z >> 4, true);
                    org.bukkit.Chunk chunk = chunkFuture.get(2, TimeUnit.SECONDS); // Shorter timeout for fallback
                    
                    // Verify the chunk was actually loaded successfully
                    if (chunk == null || !chunk.isLoaded()) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            // Find the highest solid block at this X,Z coordinate using smart scanning
            int highestY = findHighestSolidBlockSmart(world, x, z);
            if (highestY == -1) {
                continue;
            }

            if (isLocationSafe(world, x, highestY, z, configManager.getSafeBlocks(), configManager.getUnsafeBlocks())) {
                if (debugEnabled) {
                    plugin.getLogger().info("[MightyRTP] Found safe location in aggressive fallback at x=" + x + ", z=" + z + ", y=" + highestY);
                }
                // Adjust Y position to place player on top of the block, not inside it
                int adjustedY = highestY + 1;
                Location location = new Location(world, x, adjustedY, z);
                return TeleportResult.success(location);
            }
        }
        
        return TeleportResult.failure("Could not find a safe location after " + maxAttempts + " attempts");
    }
    
    /**
     * Legacy synchronous method for backward compatibility
     * @deprecated Use findSafeLocationAsync instead to avoid blocking the main thread
     */
    @Deprecated
    public TeleportResult findSafeLocation(World world, Location center) {
        return findSafeLocationSync(world, center);
    }
    
    private boolean isLocationSafe(World world, int x, int y, int z, List<Material> safeBlocks, List<Material> unsafeBlocks) {
        try {
            // Check the block at the teleport location
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockBelow = world.getBlockAt(x, y - 1, z);
            
            // Check if the block below is explicitly unsafe (like lava, fire, etc.)
            if (unsafeBlocks.contains(blockBelow.getType())) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Location rejected: Block below is explicitly unsafe - " + blockBelow.getType() + " at y=" + (y-1));
                }
                return false;
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
            // Start from a reasonable height (most terrain is below 120) and work down
            // This is much faster than scanning from max height
            for (int y = 120; y >= 32; y--) {
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
}
