# Fletch Craft

A Minecraft Fabric mod that makes the fletching table functional as a specialized crafting station for arrow-making, bow crafting, and wood processing.

## Key Feature: Works Without Client Mods

The core fletching table GUI renders entirely server-side using [sgui](https://github.com/Patbox/sgui) (bundled in the mod jar — no separate install needed). Install it on your server and vanilla clients can connect and use the fletching table without installing anything. Optionally install on the client too for a recipe book overlay.

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
- **Crossbow**: 3 sticks + 1 iron nugget + 2 string (simplified recipe)

### Wood Processing
- **Strip any log**: Place any log to get its stripped variant (oak, birch, spruce, jungle, acacia, dark oak, cherry, mangrove, pale oak, crimson, warped)
- **Sticks from stripped logs** (16): Any stripped log
- **Sticks from planks** (3): Any plank
- **Sticks from bamboo** (2): Bamboo
- **Strip bamboo block**: Bamboo block → stripped bamboo block

### Materials
- **Flint** (3): 4 gravel in 2x2 pattern
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

- Minecraft 1.21.11+
- Fabric Loader 0.18.0+
- Fabric API
- Java 21+

**Architecture**: The fletching GUI is rendered server-side via sgui. The optional client component adds a recipe book overlay by mixing into the crafting screen.

## Customization

All recipes are data-driven JSON files in `data/fletch_craft/recipe/`. Server admins can add, remove, or modify fletching recipes via datapacks without touching code.

## License

This mod is licensed under the MIT License. See [LICENSE](LICENSE) for details.
