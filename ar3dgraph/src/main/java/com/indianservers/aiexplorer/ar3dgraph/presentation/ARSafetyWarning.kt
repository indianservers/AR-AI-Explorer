package com.indianservers.aiexplorer.ar3dgraph.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** Call before composing camera/session owners; acceptance lasts only for this AR visit. */
@Composable
fun awaitARSafetyAcknowledgement(visitKey: String, onExit: () -> Unit): Boolean {
    var accepted by rememberSaveable(visitKey) { mutableStateOf(false) }
    if (!accepted) {
        BackHandler(onBack = onExit)
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                AlertDialog(
                    onDismissRequest = onExit,
                    title = { Text("AR safety warning") },
                    text = {
                        Text(
                            "Parental supervision is important. Children should use augmented reality (AR) only with a parent or guardian supervising.\n\n" +
                                "Be aware of your surroundings and physical hazards. Use AR in a clear, safe space, away from traffic, stairs, and obstacles. Stop using AR before walking or moving to another area.",
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        )
                    },
                    confirmButton = { TextButton(onClick = { accepted = true }) { Text("Continue safely") } },
                    dismissButton = { TextButton(onClick = onExit) { Text("Exit AR") } },
                    properties = DialogProperties(dismissOnClickOutside = false),
                )
            }
        }
    }
    return !accepted
}
