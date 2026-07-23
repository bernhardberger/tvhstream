package cz.preclikos.tvhstream.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import cz.preclikos.tvhstream.core.ApplianceEntryPolicy
import cz.preclikos.tvhstream.core.ApplianceLaunchRequests

class MainActivity : AppCompatActivity() {
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
