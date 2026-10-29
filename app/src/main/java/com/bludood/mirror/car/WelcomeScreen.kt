package com.bludood.mirror.car

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bludood.mirror.MirrorState
import com.bludood.mirror.ProjectionPermissionActivity
import com.bludood.mirror.R
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
                            } catch (_: Exception) {
                            }
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
            return MessageTemplate.Builder(carContext.getString(R.string.approve_on_phone))
                .build()
        }

        val startAction = Action.Builder()
            .setTitle(carContext.getString(R.string.start_mirroring))
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.ms_screen_share_24
                    )
                )
                    .build()
            )
            .setBackgroundColor(CarColor.PRIMARY)
            .apply {
                if (carContext.carAppApiLevel >= 4) setFlags(Action.FLAG_PRIMARY)
            }
            .setOnClickListener { startMirroring() }
            .build()

        return MessageTemplate.Builder(carContext.getString(R.string.car_welcome))
            .addAction(startAction)
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
