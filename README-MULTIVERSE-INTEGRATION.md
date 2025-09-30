# MightyRTP 1.0.2 - Multiverse-CommandDestination Integration

## Overview
MightyRTP 1.0.2 has been enhanced with **BetterRTP compatibility** and console command support to work seamlessly with the Multiverse-CommandDestination plugin using the existing BetterRTP configuration.

## New Features

### BetterRTP Compatibility
- **Drop-in replacement**: MightyRTP now works with existing BetterRTP portal configurations
- **Same command format**: Supports `rtp player %player% world` format
- **No configuration changes**: Use the same portal destinations you already have

### Console Command Support
- **Console execution**: The `/rtp` command can now be executed from the console
- **Cooldown bypass**: Console commands automatically bypass all cooldown restrictions
- **Permission bypass**: Console commands ignore player permission requirements
- **Dual format support**: Works with both BetterRTP and direct formats

### Command Usage

#### Console Usage (BetterRTP Format)
```
/rtp player <player> [world]
```
- `<player>`: The name of the player to teleport (required)
- `[world]`: Optional world parameter (teleports to random location in specified world)

#### Console Usage (Direct Format)
```
/rtp <player> [world]
```
- `<player>`: The name of the player to teleport (required)
- `[world]`: Optional world parameter (teleports to random location in specified world)

#### Player Usage (enhanced)
```
/rtp [player] [world]
```
- `[player]`: Optional player name to teleport (defaults to self)
- `[world]`: Optional world parameter (requires `mightyrtp.rtp.world` permission)

## Multiverse-CommandDestination Integration

### Setup
1. Install MightyRTP 1.0.2+
2. Install Multiverse-CommandDestination
3. **No additional configuration needed!**

### Portal Configuration
**Use the existing BetterRTP configuration:**
```
/mvp modify dest cmd:betterrtp -p <portalname>
```

### What Happens
When a player enters the portal:
1. The console executes: `rtp player %player% world`
2. MightyRTP recognizes the BetterRTP format
3. The player is teleported to a random safe location
4. All cooldowns and permissions are bypassed
5. The player receives normal teleport messages

## Configuration

### Permissions
- `mightyrtp.rtp` - Allows players to use RTP command
- `mightyrtp.rtp.other` - Allows players to teleport others
- `mightyrtp.rtp.world` - Allows players to use RTP command with world specification
- `mightyrtp.bypass` - Allows players to bypass cooldowns
- `mightyrtp.reload` - Allows reloading configuration

### World-Specific RTP
MightyRTP now supports teleporting players to random locations in specific worlds:

#### Console Usage (Always Available)
```bash
# Teleport player to random location in their current world
/rtp %player%

# Teleport player to random location in specific world
/rtp %player% worldname

# BetterRTP format with world specification
/rtp player %player% worldname
```

#### Player Usage (Requires Permission)
```bash
# Basic RTP in current world
/rtp

# RTP to random location in specific world (requires mightyrtp.rtp.world)
/rtp worldname

# RTP another player to specific world (requires mightyrtp.rtp.other + mightyrtp.rtp.world)
/rtp playername worldname
```

#### Portal Configuration Examples
```yaml
# Basic RTP in current world
mightyrtp:
  - 'console:rtp %player%'

# RTP to specific world
mightyrtp_world:
  - 'console:rtp %player% world'

# RTP to resource world
mightyrtp_resources:
  - 'console:rtp %player% resource_world'

# RTP to nether
mightyrtp_nether:
  - 'console:rtp %player% world_nether'
```

### Console Command Behavior
- **No cooldown checks**: Console commands always work regardless of player cooldown
- **No permission checks**: Console commands ignore player permissions
- **World restrictions**: Still respects world blacklist settings
- **Async teleportation**: Uses the same safe location finding algorithm
- **BetterRTP format**: Automatically handles `rtp player %player% world` syntax

### Title Settings
- `titles.enabled` - Enable/disable title messages when teleporting
- `titles.show-for-console` - Enable/disable title messages for console RTP commands (recommended: false for clean portal experience)

### Performance Settings
- `performance.async-teleport-search` - Use async teleport location searching (recommended: true)
- `performance.max-search-time-per-attempt` - Maximum search time per attempt in milliseconds
- `performance.fast-mode-enabled` - Enable fast mode for console commands (recommended: true)
- `performance.fast-mode-max-attempts` - Maximum attempts in fast mode (lower = faster)
- `performance.fast-mode-safety-level` - Fast mode safety level (1=basic unsafe blocks check, 2=+air above, 3=full safety)

## Compatibility

### Multiverse-CommandDestination
- ✅ **Fully compatible with existing BetterRTP portal systems**
- ✅ **No configuration changes required**
- ✅ **Drop-in replacement for BetterRTP**
- ✅ **Uses same command structure as BetterRTP**

### Other Plugins
- ✅ Maintains backward compatibility
- ✅ Player commands work exactly as before
- ✅ All existing features preserved

## Migration from BetterRTP

### Simple Replacement
1. **Stop your server**
2. **Remove BetterRTP plugin**
3. **Install MightyRTP 1.0.2+**
4. **Start your server**
5. **All portals continue working automatically**

### No Portal Changes Needed
- Existing `cmd:betterrtp` destinations work immediately
- Same portal commands and configurations
- Same player experience

## Technical Details

### Command Execution Flow
1. Portal triggers `cmd:mightyrtp` destination
2. Console executes `rtp %player%` or `rtp %player% world`
3. MightyRTP detects console execution
4. If world specified, validates world exists and checks blacklist
5. Bypasses cooldown and permission checks
6. Executes teleportation logic in target world
7. Sends appropriate messages to console and player

### Format Detection
MightyRTP automatically detects the command format:
- **BetterRTP format**: `rtp player <player> [world]` → `args[0] == "player"`
- **Direct format**: `rtp <player> [world]` → `args[0] != "player"`
- **World specification**: `args.length > 1` indicates world parameter

### World-Specific Teleportation
- **Permission required**: `mightyrtp.rtp.world` for players
- **Console access**: Always available for console commands
- **World validation**: Checks if world exists and isn't blacklisted
- **Safe location finding**: Uses same algorithm in target world
- **Cross-world teleportation**: Seamlessly handles world changes

### Cooldown Bypass
Console commands automatically set `isConsole = true`, which:
- Skips cooldown validation
- Skips permission validation
- Provides console-specific messaging
- Maintains all safety checks (world blacklist, safe location finding)

## Troubleshooting

### Common Issues
1. **Player not found**: Ensure the player is online when the portal is triggered
2. **World blacklisted**: Check MightyRTP configuration for blacklisted worlds
3. **Portal not working**: Verify the portal destination is set to `cmd:betterrtp`

### Debug Commands
- `/rtp-reload` - Reload MightyRTP configuration
- Check console for any error messages during portal activation

## Version History

### 1.0.2
- Added BetterRTP compatibility
- Added console command support
- Added Multiverse-CommandDestination integration
- **Added world-specific RTP functionality**
- **Added mightyrtp.rtp.world permission**
- Console commands bypass cooldowns and permissions
- Maintained full backward compatibility

### 1.0.1
- Initial release

### 1.0.0
- Initial release
