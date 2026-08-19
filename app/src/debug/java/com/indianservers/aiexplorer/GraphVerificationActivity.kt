package com.indianservers.aiexplorer

import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle

class GraphVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mode = intent.getStringExtra("verify_graph_mode").orEmpty()
        val expression = decodeExpression()
        setContent {
            val vm = remember { ExplorerViewModel(SavedStateHandle()) }
            LaunchedEffect(mode, expression) {
                if (mode.isNotBlank() && expression.isNotBlank()) {
                    vm.openGraphVerification(mode, expression)
                }
            }
            AIExplorerApp(vm, durableStateEnabled = false)
        }
    }

    private fun decodeExpression(): String {
        val encoded = intent.getStringExtra("verify_graph_expression_b64")
        if (!encoded.isNullOrBlank()) {
            val padded = encoded + "=".repeat((4 - encoded.length % 4) % 4)
            runCatching {
                return String(
                    Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP),
                    Charsets.UTF_8,
                )
            }
        }
        return intent.getStringExtra("verify_graph_expression").orEmpty()
    }
}
