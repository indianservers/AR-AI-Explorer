package com.indianservers.aiexplorer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberFormulaFontScalePreference(key: String): MutableState<Float> {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) {
        context.getSharedPreferences("formula_display", Context.MODE_PRIVATE)
    }
    val scale = rememberSaveable(key) {
        mutableStateOf(prefs.getFloat(key, 1f).coerceIn(.75f, 1.45f))
    }
    LaunchedEffect(key, scale.value) {
        prefs.edit().putFloat(key, scale.value.coerceIn(.75f, 1.45f)).apply()
    }
    return scale
}
