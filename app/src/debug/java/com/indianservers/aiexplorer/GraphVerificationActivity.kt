package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.workspace.MathModule
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle

class GraphVerificationActivity : ComponentActivity() {
    val verificationViewModel by lazy { ExplorerViewModel(SavedStateHandle()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mode = intent.getStringExtra("verify_graph_mode").orEmpty()
        val expression = decodeExpression()
        val arMode = intent.getStringExtra("verify_ar_workspace").orEmpty()
        val uxMode = intent.getStringExtra("verify_ux_workspace").orEmpty()
        setContent {
            val vm = remember { verificationViewModel }
            LaunchedEffect(mode, expression, arMode) {
                if (uxMode.isNotBlank()) {
                    vm.openGraphVerification("2d", "x^2")
                    vm.open(when (uxMode) {
                        "geometry2d" -> MathModule.Geometry2D
                        "geometry3d" -> MathModule.Geometry3D
                        "graph3d" -> MathModule.Graph3D
                        else -> MathModule.Graph2D
                    })
                }
                if (arMode.isNotBlank()) {
                    vm.open(when (arMode) {
                        "graph2d" -> MathModule.Graph2D
                        "geometry2d" -> MathModule.Geometry2D
                        "geometry3d" -> MathModule.Geometry3D
                        else -> MathModule.Graph3D
                    })
                    vm.open(when (arMode) {
                        "graph2d" -> MathModule.ARGraph2D
                        "geometry2d" -> MathModule.ARGeometry2D
                        "geometry3d" -> MathModule.ARGeometry3D
                        else -> MathModule.ARGraph3D
                    })
                }
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
