from pathlib import Path
ids=['speed-basic','speed-advanced','forge','kitchen','fractions','potions','balance','shapes','measure','vectors','patterns','data','chance','logic','number-bonds','times-table','decimal-harbor','math-market','integer-expedition','ratio-rangers','percent-studio','data-story','function-machine','geometry-proof','calculus-climber','matrix-mission','number-theory','combinatorics','optimization-arena']
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GameCardArtwork.kt')
s='''package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.indianservers.aiexplorer.R

internal fun gameCardArtwork(id:String):Int = when(id) {
'''
s+=''.join(f'    "{i}" -> R.drawable.game_icon_{i.replace("-","_")}\n' for i in ids)
s+='''    else -> error("Missing individual artwork for $id")
}

@Composable
internal fun GameEmblem(game:MathsGame,modifier:Modifier=Modifier) {
    Image(painterResource(gameCardArtwork(game.id)),contentDescription="${game.title} illustration",modifier=modifier)
}
'''
p.write_text(s,encoding='utf-8')
