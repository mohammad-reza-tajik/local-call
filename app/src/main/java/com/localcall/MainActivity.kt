package com.localcall

import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localcall.ui.LocalCallRoot
import com.localcall.ui.theme.LocalCallTheme

class MainActivity : ComponentActivity() {

    private val engine get() = (application as LocalCallApp).engine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Hardware volume keys control the call volume.
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        setContent {
            LocalCallTheme {
                LocalCallRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        engine.setAppVisible(true)
    }

    override fun onStop() {
        super.onStop()
        engine.setAppVisible(false)
    }
}
