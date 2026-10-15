# Fletch Craft - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## How the bench is built

The fletching table interface is a Pandorical container screen drawn on vanilla's own crafting table GUI texture at vanilla's own slot coordinates: the arrow, the result frame, the panel and every slot are the crafting table's. The result slot is taken from directly, the way a crafting table's is.

Fletching recipes ship a shaped display, so a client can see what they are without this mod installed. They carry their own recipe book category, which keeps them out of an ordinary workbench's book.

Picking a recipe in a recipe book lays it out in the bench's grid. Vanilla's route for that (`ServerboundPlaceRecipePacket`) is answered only for a `RecipeBookMenu`, which a Pandorical menu is not, so the station fills its own grid through Pandorical's `onPlaceRecipe` hook: whatever is on the bench goes back to the player first, then one of each ingredient comes out of their pack. `PlaceInVanillaMenuMixin` refuses a fletching recipe asked for at a crafting table with a message instead of letting vanilla's placement cast it to a crafting recipe. The mod checks for the `screens` capability when a player opens a fletching table and, without it, tells the player fletching requires Pandorical instead of opening anything.

The flint and obsidian items are registered through Pandorical's content API, their textures synced through its content sync, and a knife in flight is drawn with its `thrown_item` renderer.
