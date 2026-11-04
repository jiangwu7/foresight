package org.jameslick.foresight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jameslick.foresight.i18n.Localized
import org.jameslick.foresight.ui.screens.CaptureScreen
import org.jameslick.foresight.ui.screens.CardScreen
import org.jameslick.foresight.ui.screens.ConsentScreen
import org.jameslick.foresight.ui.screens.DoneScreen
import org.jameslick.foresight.ui.screens.HomeScreen
import org.jameslick.foresight.ui.screens.IntakeScreen
import org.jameslick.foresight.ui.screens.LanguageScreen
import org.jameslick.foresight.ui.screens.LiveCaptureScreen
import org.jameslick.foresight.capture.CaptureSettings
import androidx.compose.ui.platform.LocalContext
import org.jameslick.foresight.ui.screens.NewEventScreen
import org.jameslick.foresight.ui.screens.ProcessingScreen
import org.jameslick.foresight.ui.screens.ResultsScreen
import org.jameslick.foresight.ui.screens.SettingsScreen
import org.jameslick.foresight.ui.screens.Under18Screen

/** Routes follow spec §3.1. Screener-facing screens use the phone locale; participant screens use the session language. */
object Routes {
    const val HOME = "home"
    const val NEW_EVENT = "newEvent"
    const val LANGUAGE = "language"
    const val CONSENT = "consent"
    const val INTAKE = "intake"
    const val UNDER18 = "under18"
    const val CAPTURE_RIGHT = "capture/R"
    const val CAPTURE_LEFT = "capture/L"
    const val PROCESSING = "processing"
    const val RESULTS = "results"
    const val CARD = "card"
    const val DONE = "done"
    const val SETTINGS = "settings"
    const val GALLERY = "gallery"
}

@Composable
fun ForesightNav(nav: NavHostController = rememberNavController(), vm: ScreeningViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    fun restart() {
        vm.nextParticipant()
        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(vm, onStart = { nav.navigate(Routes.LANGUAGE) }, onNewEvent = { nav.navigate(Routes.NEW_EVENT) }, onSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.NEW_EVENT) {
            NewEventScreen(vm, onCreated = { nav.popBackStack() })
        }
        composable(Routes.LANGUAGE) {
            Localized(state.language) { LanguageScreen(vm, onNext = { nav.navigate(Routes.CONSENT) }) }
        }
        composable(Routes.CONSENT) {
            Localized(state.language) { ConsentScreen(vm, onAgree = { vm.giveConsent(); nav.navigate(Routes.INTAKE) }) }
        }
        composable(Routes.INTAKE) {
            Localized(state.language) {
                IntakeScreen(vm, onUnder18 = { nav.navigate(Routes.UNDER18) }, onDone = { vm.saveIntake(); nav.navigate(Routes.CAPTURE_RIGHT) })
            }
        }
        composable(Routes.UNDER18) {
            Localized(state.language) { Under18Screen(onEnd = { restart() }) }
        }
        // Phase 2: the live camera is the capture screen; the Phase 1 import flow stays for test images
        // (Settings → import mode) and for the end-to-end test hook.
        composable(Routes.CAPTURE_RIGHT) {
            val import = TestHooks.bitmapProvider != null || CaptureSettings.load(LocalContext.current).importMode
            Localized(state.language) {
                if (import) CaptureScreen(vm, eye = "R", onNext = { nav.navigate(Routes.CAPTURE_LEFT) })
                else LiveCaptureScreen(vm, eye = "R", onNext = { nav.navigate(Routes.CAPTURE_LEFT) })
            }
        }
        composable(Routes.CAPTURE_LEFT) {
            val import = TestHooks.bitmapProvider != null || CaptureSettings.load(LocalContext.current).importMode
            Localized(state.language) {
                if (import) CaptureScreen(vm, eye = "L", onNext = { nav.navigate(Routes.PROCESSING) })
                else LiveCaptureScreen(vm, eye = "L", onNext = { nav.navigate(Routes.PROCESSING) })
            }
        }
        composable(Routes.PROCESSING) {
            Localized(state.language) { ProcessingScreen(vm, onReady = { nav.navigate(Routes.RESULTS) { popUpTo(Routes.PROCESSING) { inclusive = true } } }) }
        }
        composable(Routes.RESULTS) {
            Localized(state.language) { ResultsScreen(vm, onCard = { nav.navigate(Routes.CARD) }, onDeleted = { nav.navigate(Routes.DONE) }) }
        }
        composable(Routes.CARD) {
            Localized(state.language) { CardScreen(vm, onDone = { nav.navigate(Routes.DONE) }) }
        }
        composable(Routes.DONE) {
            Localized(state.language) { DoneScreen(vm, onNext = { restart() }) }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm, onBack = { nav.popBackStack() }, onGallery = { nav.navigate(Routes.GALLERY) })
        }
        composable(Routes.GALLERY) { DesignGalleryScreen() }
    }
}
