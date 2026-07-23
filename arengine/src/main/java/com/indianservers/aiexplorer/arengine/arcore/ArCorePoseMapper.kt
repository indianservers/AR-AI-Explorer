package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.Pose
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArVector3

internal object ArCorePoseMapper {
    fun map(pose: Pose) = ArPose(
        positionMeters = ArVector3(pose.tx().toDouble(), pose.ty().toDouble(), pose.tz().toDouble()),
        orientation = ArQuaternion(
            pose.qx().toDouble(),
            pose.qy().toDouble(),
            pose.qz().toDouble(),
            pose.qw().toDouble(),
        ).normalized(),
    )
}
