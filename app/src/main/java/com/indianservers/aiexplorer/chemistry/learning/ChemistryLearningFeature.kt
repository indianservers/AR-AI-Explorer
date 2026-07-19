package com.indianservers.aiexplorer.chemistry.learning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.connectedlearning.*
@Composable fun ChemistryConnectedLearningFeature(onExit:()->Unit)=ConnectedLearningFeature(ChemistryConnectedLearningRepository.journey,ConnectedLearningColors(Color(0xFF050C12),Color(0xE80C1B24),Color(0xFF55DDE0),Color(0xFFB49CFF),Color(0xFF72E6A8),Color(0xFFFFC86B),Color(0xFFF1F8FA),Color(0xFF9CB2B8)),onExit)

