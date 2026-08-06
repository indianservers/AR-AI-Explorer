package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.presentation.NumberTheoryVisualProofViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryProgressRestorationTest {
    @Test
    fun `recent completed and saved proof ids restore from saved state`() {
        val handle = SavedStateHandle()
        val first = NumberTheoryVisualProofViewModel(handle)
        first.openTopic("even-sum")
        val proof = first.state.value.proof as NumberTheoryProofState.Ready
        repeat(proof.topic.steps.size) { first.dispatch(NumberTheoryProofAction.Next) }
        first.toggleSaved("even-sum")

        val restored = NumberTheoryVisualProofViewModel(handle).state.value
        assertTrue("even-sum" in restored.recent)
        assertTrue("even-sum" in restored.completed)
        assertTrue("even-sum" in restored.saved)
    }
}
