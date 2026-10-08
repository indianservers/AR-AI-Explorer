package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.indianservers.aiexplorer.R

internal fun gameCardArtwork(id:String):Int = when(id) {
    "speed-basic" -> R.drawable.game_icon_speed_basic
    "speed-advanced" -> R.drawable.game_icon_speed_advanced
    "forge" -> R.drawable.game_icon_forge
    "kitchen" -> R.drawable.game_icon_kitchen
    "fractions" -> R.drawable.game_icon_fractions
    "potions" -> R.drawable.game_icon_potions
    "balance" -> R.drawable.game_icon_balance
    "shapes" -> R.drawable.game_icon_shapes
    "measure" -> R.drawable.game_icon_measure
    "vectors" -> R.drawable.game_icon_vectors
    "patterns" -> R.drawable.game_icon_patterns
    "data" -> R.drawable.game_icon_data
    "chance" -> R.drawable.game_icon_chance
    "logic" -> R.drawable.game_icon_logic
    "number-bonds" -> R.drawable.game_icon_number_bonds
    "times-table" -> R.drawable.game_icon_times_table
    "decimal-harbor" -> R.drawable.game_icon_decimal_harbor
    "math-market" -> R.drawable.game_icon_math_market
    "integer-expedition" -> R.drawable.game_icon_integer_expedition
    "ratio-rangers" -> R.drawable.game_icon_ratio_rangers
    "percent-studio" -> R.drawable.game_icon_percent_studio
    "data-story" -> R.drawable.game_icon_data_story
    "function-machine" -> R.drawable.game_icon_function_machine
    "geometry-proof" -> R.drawable.game_icon_geometry_proof
    "calculus-climber" -> R.drawable.game_icon_calculus_climber
    "matrix-mission" -> R.drawable.game_icon_matrix_mission
    "number-theory" -> R.drawable.game_icon_number_theory
    "combinatorics" -> R.drawable.game_icon_combinatorics
    "optimization-arena" -> R.drawable.game_icon_optimization_arena
    else -> error("Missing individual artwork for $id")
}

@Composable
internal fun GameEmblem(game:MathsGame,modifier:Modifier=Modifier) {
    Image(painterResource(gameCardArtwork(game.id)),contentDescription="${game.title} illustration",modifier=modifier)
}
