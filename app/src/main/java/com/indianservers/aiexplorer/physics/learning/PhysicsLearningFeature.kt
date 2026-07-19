package com.indianservers.aiexplorer.physics.learning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.connectedlearning.*
@Composable fun PhysicsConnectedLearningFeature(onExit:()->Unit)=ConnectedLearningFeature(PhysicsConnectedLearningRepository.journey,ConnectedLearningColors(Color(0xFF050B14),Color(0xE80A1828),Color(0xFF62CBFF),Color(0xFFB4A0FF),Color(0xFF70E0A1),Color(0xFFFFC66B),Color(0xFFF1F7FC),Color(0xFF9BAFC0)),onExit)

