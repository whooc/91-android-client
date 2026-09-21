package com.whooc.nineone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.whooc.nineone.data.LockGate
import com.whooc.nineone.ui.NineOneApp

/**
 * The single activity. There is no WebView anywhere in this app — every screen
 * is Compose, and video is played by Media3/ExoPlayer.
 *
 * `configChanges` keeps rotation and keyboard toggles from recreating the
 * activity, which matters because the player holds an ExoPlayer instance and
 * the shorts pager holds several.
 *
 * The lifecycle pair below drives [LockGate]: going to the background stamps the
 * time, coming back decides whether the local gate has to be re-armed. Both are
 * on the activity rather than a process lifecycle observer because the app has
 * exactly one activity, and this avoids pulling in another dependency.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NineOneApp()
        }
    }

    override fun onStart() {
        super.onStart()
        LockGate.onForeground()
    }

    override fun onStop() {
        LockGate.onBackground()
        super.onStop()
    }
}
