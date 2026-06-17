package com.bludood.mirror.car

import android.content.Intent
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bludood.mirror.MirrorState

class MirrorSession : Session() {
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                MirrorState.setCarAppActive(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                MirrorState.setCarAppActive(false)
            }
        })
    }

    override fun onCreateScreen(intent: Intent): androidx.car.app.Screen {
        return WelcomeScreen(carContext)
    }
}
