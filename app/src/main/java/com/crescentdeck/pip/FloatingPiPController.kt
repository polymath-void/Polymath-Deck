package com.crescentdeck.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import com.crescentdeck.engine.quadtree.CardNode
import com.crescentdeck.engine.quadtree.QuadTreePhysicsEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Picture-in-Picture window mode, aspect ratio calculations,
 * and synchronizes the PiP overlay's bounding box as an obstacle in the QuadTree physics canvas.
 */
@Singleton
class FloatingPiPController @Inject constructor(
    private val quadTreeEngine: QuadTreePhysicsEngine
) {

    private val pipNodeId = "SYSTEM_PIP_OBSTACLE"

    fun isPiPSupported(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /**
     * Enters Picture-in-Picture mode with aspect ratio lock and remote action controls.
     */
    fun enterPiP(
        activity: Activity,
        videoWidth: Int = 16,
        videoHeight: Int = 9,
        isPlaying: Boolean = true
    ): Boolean {
        if (!isPiPSupported(activity)) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val safeWidth = if (videoWidth > 0) videoWidth else 16
            val safeHeight = if (videoHeight > 0) videoHeight else 9

            // Aspect ratio must be between 1:2.39 and 2.39:1 according to Android specs
            val ratio = Rational(safeWidth, safeHeight)

            val actions = PiPRemoteActions.buildActions(activity, isPlaying)
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .setActions(actions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
            }

            // Register obstacle in physics engine so background cards repel away
            registerPiPPhysicsObstacle(activity)

            activity.enterPictureInPictureMode(builder.build())
        } else {
            false
        }
    }

    /**
     * Registers a fixed obstacle in the QuadTree physics world
     * representing the floating PiP window's footprint.
     */
    private fun registerPiPPhysicsObstacle(activity: Activity) {
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        val pipW = 320f
        val pipH = 180f
        val pipX = screenWidth - pipW - 20f
        val pipY = screenHeight - pipH - 60f

        val obstacle = CardNode(
            id = pipNodeId,
            x = pipX,
            y = pipY,
            width = pipW,
            height = pipH,
            anchorX = pipX,
            anchorY = pipY,
            priority = 100, // Very high priority
            isFixedObstacle = true
        )
        quadTreeEngine.insertNode(obstacle)
    }

    /**
     * Removes the PiP obstacle when exiting PiP mode.
     */
    fun onExitPiP() {
        quadTreeEngine.removeNode(pipNodeId)
    }
}
