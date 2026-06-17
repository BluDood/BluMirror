package com.bludood.mirror.car

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.bludood.mirror.MirrorState
import com.bludood.mirror.ProjectionPermissionActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WelcomeScreen(carContext: CarContext) : Screen(carContext) {
    private var observeJob: Job? = null
    private var waitingJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                observeJob = MirrorState.isStreaming
                    .onEach { isStreaming ->
                        if (isStreaming && screenManager.top !is MirrorScreen) {
                            try {
                                screenManager.push(MirrorScreen(carContext))
                            } catch (_: Exception) { }
                        }
                    }
                    .launchIn(lifecycleScope)

                waitingJob = MirrorState.isWaitingForPermission
                    .onEach { invalidate() }
                    .launchIn(lifecycleScope)
            }

            override fun onPause(owner: LifecycleOwner) {
                observeJob?.cancel()
                waitingJob?.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        if (MirrorState.isWaitingForPermission.value) {
            return MessageTemplate.Builder(" ")
                .setLoading(true)
                .build()
        }

        return MessageTemplate.Builder("Welcome to BluMirror!")
            .addAction(
                Action.Builder()
                    .setTitle("Start Mirroring")
                    .setOnClickListener {
                        startMirroring()
                    }
                    .build(),
            )
            .build()
    }

    private fun startMirroring() {
        val intent = Intent(carContext, ProjectionPermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = Display.DEFAULT_DISPLAY
        }

        try {
            carContext.startActivity(intent, options.toBundle())
        } catch (_: Exception) {
            carContext.applicationContext.startActivity(intent, options.toBundle())
        }
    }
}
