package cz.preclikos.tvhstream.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cz.preclikos.tvhstream.core.ApplianceEntryPolicy
import cz.preclikos.tvhstream.core.ApplianceLaunchRequests
import cz.preclikos.tvhstream.player.PlayerSession
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val applianceLaunchRequests = ApplianceLaunchRequests()
    private val playerSession: PlayerSession by inject()
    private var isPlayerVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVHStreamTheme {
                AppRoot(
                    applianceLaunchRequests = applianceLaunchRequests,
                    onPlayerVisibilityChanged = { isPlayerVisible = it },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (ApplianceEntryPolicy.shouldCreateLaunchRequest(isPlayerVisible)) {
            applianceLaunchRequests.request()
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { playerSession.stop() }
    }
}
