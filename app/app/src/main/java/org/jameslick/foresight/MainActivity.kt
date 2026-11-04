package org.jameslick.foresight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jameslick.foresight.ui.DesignGalleryScreen
import org.jameslick.foresight.ui.theme.ForesightTheme

/**
 * Phase 0 entry point: shows the design-system gallery so the team can review tokens and components on a
 * real phone in a dark room (spec §3.10 design review gate). The screening flow replaces this in Phase 1.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForesightTheme(dark = true) {
                DesignGalleryScreen()
            }
        }
    }
}
