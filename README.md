# MightyRTP v1.0.4

> **WARNING**: Be careful using this plugin in problematic worlds like CaveBlock or cave-generated worlds, as they can cause server crashes. It is **strongly recommended** to blacklist these worlds in the configuration.

## Features

- **Smart Surface Detection**: Automatically discovers safe, random locations using intelligent terrain scanning instead of arbitrary Y levels
- **Advanced Safety System**: Comprehensive safety checks with configurable strictness levels (1-5 scale)
- **Performance Optimized**: Asynchronous teleport searching prevents server lag and TPS drops
- **World Management**: Flexible world restrictions and blacklisting
- **Cooldown System**: Configurable usage limits with bypass permissions
- **Rich Messaging**: Customizable titles, subtitles, and chat messages with MiniMessage support
- **Easy Configuration**: Extensive configuration options for all aspects of the plugin
- **Player Management**: Teleport other players with proper permission checks
- **User Experience**: Smart tab completion and intuitive command structure
- **Stable Performance**: No more server crashes from chunk loading operations

## Quick Start

### Installation
1. Download the latest JAR file from releases
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated config files

### Basic Usage
- `/rtp` - Teleport to a random safe location
- `/rtp [player]` - Teleport another player (requires permission)
- `/rtp [player] [world]` - Teleport another player to specified world (requires permission)
- `/rtp-reload` - Reload plugin configuration (requires permission)

## Commands

**`/rtp`**
- Description: Teleport to random safe location
- Permission: `mightyrtp.rtp`
- Usage: `/rtp`

**`/rtp [player]`**
- Description: Teleport another player
- Permission: `mightyrtp.rtp.other`
- Usage: `/rtp <player>`

**`/rtp [player] [world]`**
- Description: Teleport another player or yourself to specified world
- Permission: `mightyrtp.rtp.other` + `mightyrtp.rtp.world`
- Usage: `/rtp <player> <world>`

**`/rtp-reload`**
- Description: Reload configuration
- Permission: `mightyrtp.reload`
- Usage: `/rtp-reload`

## Permissions

**`mightyrtp.rtp`**
- Description: Allows players to use /rtp command
- Default: `true`

**`mightyrtp.rtp.other`**
- Description: Allows players to use /rtp command on other players
- Default: `op`

**`mightyrtp.rtp.world`**
- Description: Allows players to use /rtp command with world specification
- Default: `op`

**`mightyrtp.bypass`**
- Description: Allows players to bypass the cooldown limit
- Default: `op`

**`mightyrtp.reload`**
- Description: Allows players to reload the plugin configuration
- Default: `op`

### Config (config.yml)
```yaml
# Worlds where the /rtp command is disabled
# Case-sensitive
blacklisted-worlds:
  - "world_nether"
  - "world_the_end"

# Random teleporter distance (border limit)
# This keeps players within ±distance blocks from world center (0,0)
# Set to -1 to disable border limit
teleport-distance: 5000

# Minimum distance from world center (0,0) (prevents teleporting too close to center)
min-distance-from-spawn: 200

# Cooldown settings. Users with mightyrtp.bypass ignore this feature.
cooldown:
  # Enable/disable the cooldown system
  enabled: true
  # Number of times a user can execute the command within the cooldown window
  max-uses: 10
  # Cooldown time window in minutes
  time-window: 10

# Title settings
titles:
  # Enable/disable title messages when teleporting
  enabled: true
  # Enable/disable title messages for console RTP commands (recommended: false for clean portal experience)
  show-for-console: false

# Debug settings
debug:
  # Enable/disable debug logging for troubleshooting
  enabled: false
  # Log every Nth attempt when searching for safe locations (lower = more verbose)
  log-attempt-interval: 10

# Performance settings
performance:
  # Use async teleport location searching (recommended: true)
  # This prevents the plugin from blocking the main server thread
  async-teleport-search: true
  # Maximum search time per attempt in milliseconds (prevents hanging on single locations)
  max-search-time-per-attempt: 50
  # Fast mode for console commands - skips chunk loading for maximum speed (recommended: true)
  fast-mode-enabled: true
  # Maximum attempts in fast mode (lower = faster, but less thorough)
  fast-mode-max-attempts: 5
  # Fast mode safety level (1=basic unsafe blocks check, 2=+air above, 3=full safety)
  fast-mode-safety-level: 2

# Safety settings for teleportation
safety:
  # How strict the safety checks should be (1-5, 1=very strict, 5=very lenient)
  strictness: 3
  # Maximum number of attempts to find a safe location
  max-attempts: 50


# Unsafe block types (blocks that are considered unsafe to teleport to)
unsafe-blocks:
  - "WATER"
  - "LAVA"
  - "CACTUS"
  - "FIRE"
  - "VOID_AIR"
  - "CAVE_AIR"
  - "POWDER_SNOW"
  - "MAGMA_BLOCK"
  - "SWEET_BERRY_BUSH"
```
### Messages (messages.yml)

#### Title Configuration
```yaml
# Title message (shown when executing /rtp)
title:
  text: "<green>Teleporting please wait...</green>"
  # Note: Color and formatting are handled in the text field using tags

# Subtitle message (shown below the title)
subtitle:
  text: "<aqua><italic>Good luck out there!</italic></aqua>"
  # Note: Color and formatting are handled in the text field using tags
```

#### Chat Messages
```yaml
messages:
  teleporting: "<green>Finding a safe location...</green>"
  teleported: "<green>You have been teleported to a random location!</green>"
  no-safe-location: "<red>Could not find a safe location. Please try again!</red>"
  world-blacklisted: "<red>Random teleport is disabled in this world!</red>"
  reload-success: "<green>Configuration reloaded successfully!</green>"
  reload-failed: "<red>Failed to reload configuration!</red>"
  no-permission: "<red>You don't have permission to use this command!</red>"
  cooldown-exceeded: "<red>You have exceeded the cooldown limit! You can use this command again in</red> <yellow>{time}.</yellow>"
  cooldown-remaining: "<red>You can use this command {remaining} more times within the next {time} minutes.</red>"
  bypass-active: "<gold>Bypass permission active - unlimited teleports!</gold>"
  player-not-found: "<red>Player not found: {player}</red>"
  teleporting-other: "<green>Teleporting {player} to a random location...</green>"
  teleported-other: "<green>Successfully teleported {player} to a random location!</green>"
  no-safe-location-other: "<red>Could not find a safe location for {player}</red>"
```

## Technical Details

### Smart Surface Detection
The plugin uses intelligent terrain scanning instead of arbitrary Y-level checking:
- **Height Range**: Scans from Y=120 down to Y=32 (covers 99% of terrain)
- **Surface Finding**: Locates the actual highest solid block at each X,Z coordinate
- **Performance**: Much faster than scanning from world max height

### Asynchronous Operation
- **Chunk Loading**: Uses `getChunkAtAsync()` to prevent server crashes
- **Location Search**: Runs on async threads to avoid blocking main server thread
- **Thread Safety**: Properly handles Bukkit's thread safety requirements
- **Timeout Protection**: Built-in timeouts prevent infinite waiting

### Player Positioning
- **Surface Placement**: Players are teleported to Y+1 (on top of blocks)
- **No Embedding**: Prevents players from being placed inside blocks
- **Collision Safe**: Ensures proper player positioning for all teleportations

## Troubleshooting

### Common Issues

**Plugin fails to find safe locations:**
- Increase `max-attempts` in config
- Verify world generation is working properly
- Consider adjusting `safety.strictness` (lower = more strict, higher = more lenient)

**Server lag during teleportation:**
- Ensure `async-teleport-search: true` is enabled
- Reduce `max-attempts` if needed
- Check server performance settings

**Chunk loading errors:**
- Verify server has sufficient RAM
- Check if world generation is enabled
- Ensure proper server permissions

### Performance Tips
- **Normal worlds**: Default settings work well
- **Complex terrain**: Increase `max-attempts` to 75-100
- **Cave worlds**: Set `safety.strictness` to 4-5 for more lenient placement, BUT HIGHLY RECOMMENDED TO BLOCK THESE WORLDS.
- **High-traffic servers**: Keep `max-attempts` moderate (50-75) to balance success rate and performance

## Support
If you need help or have suggestions, please open an issue on the GitHub repository or join the linked Discord and navigate to # plugin-help

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

---