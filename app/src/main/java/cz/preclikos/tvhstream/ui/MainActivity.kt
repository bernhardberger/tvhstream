package cz.preclikos.tvhstream.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cz.preclikos.tvhstream.core.ApplianceEntryPolicy
import cz.preclikos.tvhstream.core.ApplianceLaunchRequests

class MainActivity : ComponentActivity() {
    private val applianceLaunchRequests = ApplianceLaunchRequests()
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
}
