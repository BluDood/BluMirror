package com.bludood.mirror.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bludood.mirror.MirrorState
import com.bludood.mirror.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MirrorScreen(
    carContext: CarContext
) : Screen(carContext), SurfaceCallback {
    private var observeJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java)
                    .setSurfaceCallback(this@MirrorScreen)

                observeJob = MirrorState.isStreaming
                    .onEach { isStreaming ->
                        if (!isStreaming) {
                            if (screenManager.top is MirrorScreen && screenManager.stackSize > 1) {
                                screenManager.pop()
                            }
                        }
                    }
                    .launchIn(lifecycleScope)
            }

            override fun onPause(owner: LifecycleOwner) {
                observeJob?.cancel()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
            }
        })
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(carContext.getString(R.string.stop_mirroring))
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(
                                        carContext,
                                        R.drawable.ms_stop_24
                                    )
                                )
                                    .build()
                            )
                            .setOnClickListener {
                                MirrorState.stopAll(carContext)
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        if (!surface.isValid) return

        MirrorState.setCarAppActive(true)
        MirrorState.setSurface(
            surface = surface,
            width = surfaceContainer.width,
            height = surfaceContainer.height,
            dpi = surfaceContainer.dpi
        )
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        MirrorState.clearSurface()
    }
}
