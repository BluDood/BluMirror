package com.bludood.mirror

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bludood.mirror.ui.theme.BluMirrorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MirrorState.init(applicationContext)

        setContent {
            BluMirrorTheme {
                MainScreen(
                    onStart = {
                        val intent = Intent(this, ProjectionPermissionActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(intent)
                    }
                ) {
                    MirrorState.stopAll(this)
                }
            }
        }
    }
}

@Composable
fun MainScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val isStreaming by MirrorState.isStreaming.collectAsState()
    val isCarAppActive by MirrorState.isCarAppActive.collectAsState()
    val isCarConnected by MirrorState.isCarConnected.collectAsState()
    val isWaitingForPermission by MirrorState.isWaitingForPermission.collectAsState()

    MirrorContent(
        isStreaming = isStreaming,
        isCarAppActive = isCarAppActive,
        isCarConnected = isCarConnected,
        isWaitingForPermission = isWaitingForPermission,
        onStart = onStart,
        onStop = onStop
    )
}

@Composable
internal fun MirrorContent(
    isStreaming: Boolean,
    isCarAppActive: Boolean,
    isCarConnected: Boolean,
    isWaitingForPermission: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val pageScroll = rememberScrollState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(pageScroll, enabled = pageScroll.maxValue > 0)
                    .heightIn(min = maxHeight)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        when {
                            !isCarConnected -> R.string.connect_android_auto
                            isStreaming && isCarAppActive -> R.string.mirroring_active
                            isStreaming -> R.string.open_on_car
                            isWaitingForPermission -> R.string.approve_on_phone
                            else -> R.string.ready_to_mirror
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (isCarConnected) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = if (isStreaming) onStop else onStart,
                        enabled = isStreaming || !isWaitingForPermission,
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.contentPaddingFor(
                            ButtonDefaults.MinHeight,
                            hasStartIcon = true
                        )
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isStreaming) R.drawable.ms_stop_24 else R.drawable.ms_screen_share_24
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight))
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                        Text(stringResource(if (isStreaming) R.string.stop_mirroring else R.string.start_mirroring))
                    }
                }
            }
        }
    }
}
