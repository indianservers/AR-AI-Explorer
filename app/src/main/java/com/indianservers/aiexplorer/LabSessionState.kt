package com.indianservers.aiexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.indianservers.aiexplorer.workspace.MathModule

/** Keeps editable lab parameters alive across workspace switches and activity recreation. */
@Composable
internal fun rememberLabText(
    vm: ExplorerViewModel,
    module: MathModule,
    key: String,
    default: String,
): MutableState<String> {
    val revision = vm.labSessionRevision
    val backing = remember(vm, module, key, revision) { mutableStateOf(vm.labValue(module, key, default)) }
    return remember(backing, vm, module, key) {
        object : MutableState<String> by backing {
            override var value: String
                get() = backing.value
                set(next) {
                    backing.value = next
                    vm.setLabValue(module, key, next)
                }
        }
    }
}
