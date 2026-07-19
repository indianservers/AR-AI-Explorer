package com.indianservers.aiexplorer.biology.learning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.connectedlearning.*
@Composable fun BiologyConnectedLearningFeature(onExit:()->Unit)=ConnectedLearningFeature(BiologyConnectedLearningRepository.journey,ConnectedLearningColors(Color(0xFF050C0C),Color(0xEB0E2020),Color(0xFF70E0A1),Color(0xFF60DDE5),Color(0xFFB5A1FF),Color(0xFFFFC970),Color(0xFFF2FAF7),Color(0xFFA1B8B1)),onExit)

