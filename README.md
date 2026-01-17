# Fletch Craft

A Minecraft Fabric mod that makes the fletching table functional as a specialized crafting station for arrow-making, bow crafting, and wood processing.

## 🎯 Key Feature: Works Server-Only!

**Install this mod on your server only** - vanilla clients can connect without any mods and use the fletching table perfectly! The GUI renders server-side, so all players see and can use the fletching interface.

## Features

- **Functional Fletching Table**: Right-click the fletching table to open a 3x3 crafting interface
- **Server-Side GUI**: No client mods required - vanilla players can use all features
- **Optional Client Enhancement**: If installed on client too, adds a recipe book with all fletching recipes
- **Shift-Click Support**: With client mod, shift-click recipes to auto-fill the grid

## Recipes

### Arrows & Ammunition
- **Arrows** (16): 3 flint + 3 sticks + 3 feathers
- **Spectral Arrows** (3): 3 glowstone dust + 3 arrows

### Weapons
- **Bow**: 3 sticks + 3 string (2x3 pattern)
- **Crossbow**: 3 sticks + 2 iron nuggets + 2 string (simplified recipe)

### Wood Processing
- **Strip any log**: Place any log to get its stripped variant
- **Sticks from stripped logs** (16): Any stripped log
- **Sticks from planks** (3): Any plank
- **Sticks from bamboo** (2): Bamboo
- **Strip bamboo block**: Bamboo block → stripped bamboo block

### Materials
- **Flint**: 4 gravel in 2x2 pattern
- **String** (9): Any wool color

### Archery
- **Target Block**: 1 redstone + 1 hay bale

## Installation

### Server-Only (Recommended)
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) on your server
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) on your server
3. Download the mod jar and place it in your server's `mods` folder
4. **That's it!** Vanilla clients can now connect and use the fletching table

### Client + Server (For Recipe Book)
1. Follow the server installation above
2. Optionally install the mod on your client for the recipe book feature

## Requirements

- Minecraft 1.21.x
- Fabric Loader 0.16.0+
- Fabric API
- Java 21+

**Server Compatibility Note**: This mod uses server-side GUI rendering (sgui), which means vanilla clients can connect to your server and use the fletching table without installing any mods!

## License

This mod is licensed under the MIT License. See [LICENSE](LICENSE) for details.
