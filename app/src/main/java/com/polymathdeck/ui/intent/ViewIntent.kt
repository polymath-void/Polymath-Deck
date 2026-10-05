package com.polymathdeck.ui.intent

/**
 * Model-View-Intent (MVI) user actions and system intents
 * dispatched to the DeckViewModel single source of truth.
 */
sealed class ViewIntent {
    data class DragStart(val nodeId: String) : ViewIntent()
    data class UpdatePosition(val nodeId: String, val deltaX: Float, val deltaY: Float) : ViewIntent()
    data class DragEnd(val nodeId: String) : ViewIntent()
    data class ResizeStart(val nodeId: String) : ViewIntent()
    data class UpdateSize(val nodeId: String, val newWidth: Float, val newHeight: Float) : ViewIntent()
    data class ResizeEnd(val nodeId: String) : ViewIntent()
    data class ToggleImmersive(val nodeId: String) : ViewIntent()
    data class MinimizeNode(val nodeId: String) : ViewIntent()
    data class RestoreNode(val nodeId: String) : ViewIntent()
    data class AddNodeFromUri(val uri: String) : ViewIntent()
    data class RemoveNode(val nodeId: String) : ViewIntent()
    data class PanViewport(val deltaX: Float, val deltaY: Float) : ViewIntent()
    data class ZoomViewport(val zoomFactor: Float) : ViewIntent()
}
