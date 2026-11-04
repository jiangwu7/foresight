package org.jameslick.foresight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jameslick.foresight.ui.ForesightNav
import org.jameslick.foresight.ui.theme.ForesightTheme

/** Entry point: the screening flow (spec §3.1). Dark theme always; the capture screen needs it (§3.2). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForesightTheme(dark = true) {
                ForesightNav()
            }
        }
    }
}
