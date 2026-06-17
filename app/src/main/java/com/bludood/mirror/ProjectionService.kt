package com.bludood.mirror

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.app.PendingIntent
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ProjectionService : Service() {
    private var projection: MediaProjection? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("BluMirror")
            .setContentText("Screen mirroring is active")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projection != null) return START_STICKY
        if (intent == null) return START_NOT_STICKY

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        try {
            projection = manager.getMediaProjection(resultCode, data).also { mediaProjection ->
                if (mediaProjection != null) {
                    mediaProjection.registerCallback(
                        object : MediaProjection.Callback() {
                            override fun onStop() {
                                MirrorState.stopProjection()
                                stopSelf()
                            }
                        },
                        null
                    )

                    MirrorState.setProjection(mediaProjection)
                    Toast.makeText(this, "Screen mirroring started", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Toast.makeText(this, "Screen mirroring stopped", Toast.LENGTH_SHORT).show()
        MirrorState.stopProjection()
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BluMirror",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "blumirror"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_DATA = "data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ProjectionService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)

            context.startForegroundService(intent)
        }
    }
}
