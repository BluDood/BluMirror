package com.bludood.mirror

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class ProjectionPermissionActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        MirrorState.setWaitingForPermission(false)
        val data = result.data

        if (result.resultCode == RESULT_OK && data != null) {
            ProjectionService.start(
                context = this,
                resultCode = result.resultCode,
                data = data
            )
        }

        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MirrorState.setWaitingForPermission(true)

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
}
