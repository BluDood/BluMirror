package com.bludood.mirror

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bludood.mirror.ui.theme.BluMirrorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Welcome to BluMirror!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    !isCarConnected -> "Please connect your phone to Android Auto to start mirroring."
                    isStreaming && isCarAppActive -> "Stream is running"
                    isStreaming -> "Streaming started. Open the BluMirror app on your car's screen to continue."
                    else -> "Ready to mirror"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isCarConnected) {
                if (isStreaming) {
                    Button(onClick = onStop) {
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onStart) {
                        Text("Start")
                    }
                }
            }
        }
    }
}
