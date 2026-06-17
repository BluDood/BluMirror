package com.bludood.mirror

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface
import androidx.car.app.connection.CarConnection
import com.bludood.mirror.car.MirrorRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MirrorState {
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isCarAppActive = MutableStateFlow(false)
    val isCarAppActive: StateFlow<Boolean> = _isCarAppActive.asStateFlow()

    private val _isWaitingForPermission = MutableStateFlow(false)
    val isWaitingForPermission: StateFlow<Boolean> = _isWaitingForPermission.asStateFlow()

    private val _isCarConnected = MutableStateFlow(false)
    val isCarConnected: StateFlow<Boolean> = _isCarConnected.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        CarConnection(context).type.observeForever { type ->
            _isCarConnected.value = type != CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        }
    }

    fun setCarAppActive(active: Boolean) {
        if (_isCarAppActive.value != active) {
            _isCarAppActive.value = active
        }
    }

    fun setWaitingForPermission(waiting: Boolean) {
        _isWaitingForPermission.value = waiting
    }

    var renderer: MirrorRenderer? = null
    var mediaProjection: MediaProjection? = null
    var carSurface: Surface? = null

    var surfaceWidth: Int = 0
    var surfaceHeight: Int = 0
    var surfaceDpi: Int = 160

    private var currentProjection: MediaProjection? = null

    fun setSurface(surface: Surface, width: Int, height: Int, dpi: Int) {
        if (!surface.isValid) return
        
        carSurface = surface
        surfaceWidth = width
        surfaceHeight = height
        surfaceDpi = dpi
        setCarAppActive(true)
        startRenderer()
    }

    fun clearSurface() {
        carSurface = null
        setCarAppActive(false)
        renderer?.updateSurface(null)
    }

    fun setProjection(p: MediaProjection) {
        mediaProjection = p
        _isStreaming.value = true
        startRenderer()
    }

    fun isProjectionActive(): Boolean {
        return mediaProjection != null
    }

    fun stopProjection() {
        mediaProjection?.stop()
        mediaProjection = null
        currentProjection = null
        _isStreaming.value = false
        stopRenderer()
    }

    fun startRenderer() {
        val projection = mediaProjection ?: return
        val surface = carSurface ?: return
        if (!surface.isValid) return

        val r = renderer
        if (r != null && projection == currentProjection) {
            r.updateSurface(surface)
            r.resize(surfaceWidth, surfaceHeight, surfaceDpi)
            _isCarAppActive.value = true
            return
        }

        stopRenderer()
        currentProjection = projection

        try {
            renderer = MirrorRenderer(
                mediaProjection = projection,
                surface = surface,
                width = surfaceWidth,
                height = surfaceHeight,
                dpi = surfaceDpi,
            ).also { it.start() }
            _isCarAppActive.value = true
        } catch (_: Exception) {
            renderer = null
            currentProjection = null
        }
    }

    fun stopRenderer() {
        renderer?.stop()
        renderer = null
        currentProjection = null
    }

    fun stopAll(context: Context? = null) {
        stopRenderer()
        stopProjection()
        clearSurface()
        _isCarAppActive.value = false

        context?.stopService(Intent(context, ProjectionService::class.java))
    }
}
