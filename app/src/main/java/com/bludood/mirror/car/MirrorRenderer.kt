package com.bludood.mirror.car

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.view.Surface

class MirrorRenderer(
    private val mediaProjection: MediaProjection,
    private var surface: Surface?,
    private var width: Int,
    private var height: Int,
    private var dpi: Int
) {
    private var virtualDisplay: VirtualDisplay? = null

    fun start() {
        if (virtualDisplay != null) return

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "BluMirror",
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            dpi.coerceAtLeast(160),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            surface,
            null,
            null
        )
    }

    fun updateSurface(newSurface: Surface?) {
        if (surface != newSurface) {
            surface = newSurface
            virtualDisplay?.surface = newSurface
        }
    }

    fun resize(newWidth: Int, newHeight: Int, newDpi: Int) {
        if (width != newWidth || height != newHeight || dpi != newDpi) {
            width = newWidth
            height = newHeight
            dpi = newDpi
            virtualDisplay?.resize(
                width.coerceAtLeast(1),
                height.coerceAtLeast(1),
                dpi.coerceAtLeast(160)
            )
        }
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
