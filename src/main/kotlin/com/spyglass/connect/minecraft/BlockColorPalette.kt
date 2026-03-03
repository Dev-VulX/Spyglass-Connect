package com.spyglass.connect.minecraft

/**
 * Map ~200 common Minecraft block IDs to RGB colors for overhead map rendering.
 * Colors approximate the block's appearance from above.
 */
object BlockColorPalette {

    /** Get the color for a block ID. Returns a default gray for unknown blocks. */
    fun getColor(blockId: String): Int {
        return COLORS[blockId.removePrefix("minecraft:")] ?: DEFAULT_COLOR
    }

    private const val DEFAULT_COLOR = 0xFF808080.toInt()

    private val COLORS = mapOf(
        // Terrain
        "grass_block" to 0xFF7CBD6B.toInt(),
        "short_grass" to 0xFF7CBD6B.toInt(),
        "tall_grass" to 0xFF7CBD6B.toInt(),
        "dirt" to 0xFF866043.toInt(),
        "coarse_dirt" to 0xFF77553B.toInt(),
        "rooted_dirt" to 0xFF886244.toInt(),
        "podzol" to 0xFF6A4E31.toInt(),
        "mycelium" to 0xFF6F6265.toInt(),
        "mud" to 0xFF3C3837.toInt(),
        "farmland" to 0xFF6D4325.toInt(),
        "dirt_path" to 0xFF947B49.toInt(),
        "sand" to 0xFFDBCFA3.toInt(),
        "red_sand" to 0xFFBE6621.toInt(),
        "gravel" to 0xFF858381.toInt(),
        "clay" to 0xFF9EA3AE.toInt(),
        "soul_sand" to 0xFF51412E.toInt(),
        "soul_soil" to 0xFF4C3C29.toInt(),

        // Stone
        "stone" to 0xFF7D7D7D.toInt(),
        "granite" to 0xFF9A6C51.toInt(),
        "diorite" to 0xFFBCBCBE.toInt(),
        "andesite" to 0xFF888888.toInt(),
        "deepslate" to 0xFF505050.toInt(),
        "tuff" to 0xFF6C6C5F.toInt(),
        "calcite" to 0xFFDFDDD4.toInt(),
        "dripstone_block" to 0xFF866A5B.toInt(),
        "cobblestone" to 0xFF7F7F7F.toInt(),
        "mossy_cobblestone" to 0xFF6D7D5A.toInt(),
        "bedrock" to 0xFF333333.toInt(),
        "obsidian" to 0xFF0F0A18.toInt(),
        "crying_obsidian" to 0xFF200A3A.toInt(),
        "basalt" to 0xFF4C4C4E.toInt(),
        "smooth_basalt" to 0xFF484849.toInt(),
        "blackstone" to 0xFF2C272E.toInt(),
        "end_stone" to 0xFFDBDE9F.toInt(),
        "netherrack" to 0xFF6E3430.toInt(),

        // Water & Ice
        "water" to 0xFF3F76E4.toInt(),
        "ice" to 0xFF92B2FD.toInt(),
        "packed_ice" to 0xFF8DB4FE.toInt(),
        "blue_ice" to 0xFF74A7F9.toInt(),
        "snow_block" to 0xFFF9FEFE.toInt(),
        "snow" to 0xFFF9FEFE.toInt(),
        "powder_snow" to 0xFFF8FDFD.toInt(),

        // Ores
        "coal_ore" to 0xFF6B6B6B.toInt(),
        "iron_ore" to 0xFF89796A.toInt(),
        "gold_ore" to 0xFF8F8154.toInt(),
        "diamond_ore" to 0xFF7BBFB2.toInt(),
        "emerald_ore" to 0xFF6D9561.toInt(),
        "lapis_ore" to 0xFF616C8C.toInt(),
        "redstone_ore" to 0xFF8B5050.toInt(),
        "copper_ore" to 0xFF7E6D56.toInt(),

        // Wood
        "oak_log" to 0xFF6B5539.toInt(),
        "spruce_log" to 0xFF3A2810.toInt(),
        "birch_log" to 0xFFD1C8A5.toInt(),
        "jungle_log" to 0xFF575217.toInt(),
        "acacia_log" to 0xFF676158.toInt(),
        "dark_oak_log" to 0xFF3E2A12.toInt(),
        "mangrove_log" to 0xFF6E4838.toInt(),
        "cherry_log" to 0xFF34182E.toInt(),
        "oak_planks" to 0xFFA88754.toInt(),
        "spruce_planks" to 0xFF735431.toInt(),
        "birch_planks" to 0xFFC5B77C.toInt(),
        "jungle_planks" to 0xFFA57546.toInt(),
        "acacia_planks" to 0xFFA85A2B.toInt(),
        "dark_oak_planks" to 0xFF42290F.toInt(),
        "mangrove_planks" to 0xFF773737.toInt(),
        "cherry_planks" to 0xFFE4C7B5.toInt(),
        "oak_leaves" to 0xFF4A7A26.toInt(),
        "spruce_leaves" to 0xFF3E6126.toInt(),
        "birch_leaves" to 0xFF5E8A30.toInt(),
        "jungle_leaves" to 0xFF3F8B1F.toInt(),
        "acacia_leaves" to 0xFF467A26.toInt(),
        "dark_oak_leaves" to 0xFF3A6A18.toInt(),
        "mangrove_leaves" to 0xFF4E8C2C.toInt(),
        "cherry_leaves" to 0xFFF2BAC9.toInt(),
        "azalea_leaves" to 0xFF577A38.toInt(),

        // Plants & Vegetation
        "fern" to 0xFF5B8A32.toInt(),
        "dandelion" to 0xFF7CBD6B.toInt(),
        "poppy" to 0xFF7CBD6B.toInt(),
        "lily_pad" to 0xFF208030.toInt(),
        "cactus" to 0xFF5A8124.toInt(),
        "sugar_cane" to 0xFF7DAC64.toInt(),
        "bamboo" to 0xFF5E801B.toInt(),
        "vine" to 0xFF4A7A26.toInt(),
        "seagrass" to 0xFF278028.toInt(),

        // Nether
        "nether_bricks" to 0xFF2C1517.toInt(),
        "crimson_nylium" to 0xFF832126.toInt(),
        "warped_nylium" to 0xFF2B6C63.toInt(),
        "crimson_stem" to 0xFF6F2138.toInt(),
        "warped_stem" to 0xFF3A635B.toInt(),
        "glowstone" to 0xFFAB8854.toInt(),
        "magma_block" to 0xFF943517.toInt(),
        "ancient_debris" to 0xFF654437.toInt(),
        "nether_gold_ore" to 0xFF6E3430.toInt(),
        "nether_quartz_ore" to 0xFF6E3430.toInt(),
        "shroomlight" to 0xFFF09840.toInt(),

        // End
        "end_stone_bricks" to 0xFFDADD9E.toInt(),
        "purpur_block" to 0xFFA677A6.toInt(),
        "chorus_plant" to 0xFF5C3461.toInt(),

        // Building
        "stone_bricks" to 0xFF7B7B7B.toInt(),
        "bricks" to 0xFF966457.toInt(),
        "sandstone" to 0xFFD7CB8A.toInt(),
        "red_sandstone" to 0xFFA45521.toInt(),
        "prismarine" to 0xFF63A39B.toInt(),
        "dark_prismarine" to 0xFF335B4D.toInt(),
        "terracotta" to 0xFF985B43.toInt(),
        "white_terracotta" to 0xFFD1B2A1.toInt(),
        "orange_terracotta" to 0xFFA15325.toInt(),
        "white_concrete" to 0xFFCFD5D6.toInt(),
        "black_concrete" to 0xFF080A0F.toInt(),

        // Wool
        "white_wool" to 0xFFE9ECEC.toInt(),
        "orange_wool" to 0xFFF07613.toInt(),
        "magenta_wool" to 0xFFBD44B3.toInt(),
        "light_blue_wool" to 0xFF3AAFD9.toInt(),
        "yellow_wool" to 0xFFF8C627.toInt(),
        "lime_wool" to 0xFF70B919.toInt(),
        "pink_wool" to 0xFFF0A3B2.toInt(),
        "gray_wool" to 0xFF3E4447.toInt(),
        "light_gray_wool" to 0xFF8E8E86.toInt(),
        "cyan_wool" to 0xFF158991.toInt(),
        "purple_wool" to 0xFF792AAC.toInt(),
        "blue_wool" to 0xFF353A9E.toInt(),
        "brown_wool" to 0xFF724728.toInt(),
        "green_wool" to 0xFF546D1B.toInt(),
        "red_wool" to 0xFFA12722.toInt(),
        "black_wool" to 0xFF141519.toInt(),

        // Misc
        "lava" to 0xFFD4590A.toInt(),
        "air" to 0x00000000,
        "cave_air" to 0x00000000,
        "void_air" to 0x00000000,
        "glass" to 0xC0D6EFF5.toInt(),
        "iron_block" to 0xFFDCDCDC.toInt(),
        "gold_block" to 0xFFF9D849.toInt(),
        "diamond_block" to 0xFF61DBD1.toInt(),
        "emerald_block" to 0xFF2FBD40.toInt(),
        "lapis_block" to 0xFF1D47A5.toInt(),
        "redstone_block" to 0xFFB01E02.toInt(),
        "copper_block" to 0xFFC06838.toInt(),
        "amethyst_block" to 0xFF8561C0.toInt(),
        "moss_block" to 0xFF597529.toInt(),
        "sculk" to 0xFF0C1C29.toInt(),
        "hay_block" to 0xFFA68C14.toInt(),
        "melon" to 0xFF698924.toInt(),
        "pumpkin" to 0xFFC78024.toInt(),
    )
}
