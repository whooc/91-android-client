package com.whooc.nineone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.whooc.nineone.ui.NineOneApp

/**
 * The single activity. There is no WebView anywhere in this app — every screen
 * is Compose, and video is played by Media3/ExoPlayer.
 *
 * `configChanges` keeps rotation and keyboard toggles from recreating the
 * activity, which matters because the player holds an ExoPlayer instance and
 * the shorts pager holds several.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NineOneApp()
        }
    }
}
