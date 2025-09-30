# Changelog

## [1.0.4] - 2025-09-24

### Fixed
- **Cooldown off-by-one error** - Fixed cooldown limiter allowing 1 extra RTP beyond the configured limit

## [1.0.3] - 2025-09-08

### Fixed
- **Nether roof teleportation issue** - Fixed players being teleported to the Nether roof (Y=127+)
- Added dimension-specific height limits to prevent scanning above the Nether roof
- Nether RTP now properly stays below Y=120 to avoid the bedrock roof

### Technical
- Added `getMaxSearchHeight()` and `getMinSearchHeight()` methods for dimension-specific height limits
- Updated `findHighestSolidBlockSmart()` and `findHighestSolidBlockFast()` to respect dimension limits
- **Nether**: Scans from Y=120 down to Y=32 (avoids roof at Y=127)
- **Overworld**: Scans from Y=120 down to Y=32 (unchanged behavior)
- **End**: Scans from Y=100 down to Y=0 (appropriate for End terrain)

## [1.0.2] - 2025-08-21

### Added
- **Multiverse-CommandDestination compatibility** - Full integration with portal systems using [world] arguments and console command support for `/rtp` command
- **World-specific RTP functionality** - `/rtp %player% [world]` to teleport to specific worlds
- New permission `mightyrtp.rtp.world` for world-specific teleportation
- Enhanced command usage with world parameter support

### Changed
- Updated plugin version to 1.0.2
- Enhanced plugin description to mention Multiverse-CommandDestination compatibility and world support
- Improved command usage documentation
- Refactored RTPCommand for better console/player command handling
- **Console commands now work seamlessly with Multiverse portals, (Multiverse-CommandDestination required)**

### Technical
- Added `executeRTP()` method for shared teleportation logic
- Added `executeRTPInWorld()` method for world-specific teleportation
- Console execution detection and handling
- Portal command format detection (`args[0] == "player"`)
- World parameter handling and validation
- Cooldown bypass for console commands
- Permission bypass for console commands
- **Performance optimizations**: Reduced max attempts, faster chunk loading, fast mode for console
- **Fast mode**: New fast mode that skips chunk loading for maximum speed
- **Fast mode configuration**: Configurable fast mode settings for better performance
- **Fast mode safety levels**: Configurable safety levels (1=basic unsafe blocks check, 2=+air above, 3=full safety)
- **Console title suppression**: Console RTP commands no longer show titles to players (configurable)
- Maintained full backward compatibility

## [1.0.1] - 2025-08-15

### Added
- Initial release with basic RTP functionality
- Cooldown system
- Permission system
- World blacklist support
- Safe location finding

## [1.0.0] - 2025-08-11

### Added
- Initial plugin structure
- Basic RTP command implementation
