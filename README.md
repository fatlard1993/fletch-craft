# Fletch Craft

A Minecraft Fabric mod that makes the fletching table functional as a specialized crafting station for arrow-making, bow crafting, and wood processing.

## Features

- **Functional Fletching Table**: Right-click the fletching table to open a 3x3 crafting interface
- **Server-Side Compatible**: Install on server only - vanilla clients can connect and use the fletching table
- **Optional Client Enhancement**: When installed client-side, adds a custom recipe book showing fletching recipes
- **Shift-Click Support**: Shift-click recipes to fill the grid with maximum available ingredients

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

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.x
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the mod jar and place it in your `mods` folder

## Requirements

- Minecraft 1.21.x
- Fabric Loader 0.16.0+
- Fabric API
- Java 21+

Note: The mod includes sgui for server-side GUI rendering, allowing vanilla clients to use the fletching table without needing the mod installed.

## License

This mod is licensed under the MIT License. See [LICENSE](LICENSE) for details.
